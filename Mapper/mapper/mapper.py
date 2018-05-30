# -*- coding: utf-8 -*-
import xml.etree.ElementTree as ET
from shutil import copy
import argparse
import os
import codecs
from utility.helper import get_child_parent_mapper, open_file

from utility.LocalAlignment import create_score_matrix, get_start_alignment_id
import re
import config

STOPWORDS = """
,
a 
about 
and
an 
are 
as 
at 
be 
by 
com 
for 
from
how
in 
is 
it 
of 
on 
or 
that
the 
this
to 
was 
what 
when
where
who 
will 
with
the
"""
SPL_CHAR = [',', ';', ':', 'tel', '(', ')']
contextual_words = {
    'email': ['corresponding', 'email', 'e-mail', 'address', 'mail', 'addresses'],
    'phone': ['telephone', 'ph', 'tel', 'tele', 'numbers', 'number','tel/fax','tel:/fax', 'cellphone','mobile',\
              'no', 'and', 'tel','phone','tel./fax'],
    'docAuthor': ['corresponding', 'authors', 'authors',"authors'",'to','correspondence','information','&amp;','dr.', '-dr.','phd'],\
    'address': ['and', '&amp;'],
    'affiliation': ['institutional', 'affiliation'],
    'ptr': ['url']
}

EMAIL_REGEX = re.compile(r"[^@]+@[^@]+\.[^@]+")
Email_dict = {}


# def get_match_score(str1, str2):
#     sim, (start, end) = edit_distance(str1, str2)
#     return start, end, sim


def get_poss_variations(name_text):
    name_tokens = name_text.split()
    if len(name_tokens) > 1:
        shortened_name = ' '.join([tok[0].upper()+'.' for tok in name_tokens[:-1]])+' '+name_tokens[-1]
        if name_text != shortened_name:
            return [name_text, shortened_name]
    return [name_text]


def get_tok_id(seq, cid, end =False):
    """
    assuming tokens are separated by single space
    :param seq target sequence:
    :param cid last macthed character:
    :return number of spaces (last matched token id):
    """
    nb = 0
    c=''
    for c in seq[:cid+1]:
        if c == ' ':
            nb += 1
    if end == True and (c==' ' or c == '('):
        nb -= 1
    return nb


def remove_email_text(text, tag_tuples):
    tags = [m[1] for m in tag_tuples]
    token_seq = text.split()
    if 'email' not in [Mapper.rev_map[tag] for tag in tags]:
        for i, token in enumerate(token_seq):
            if EMAIL_REGEX.match(token):
                new_token = 'electronic_mail'+str(i)
                token_seq[i] = new_token
                Email_dict[new_token] = token
    return ' '.join(token_seq)


def has_digit(text):
    ctr = 0
    for c in text:
        if c.isdigit():
            ctr +=1
    if 'office' in text.lower():
        if ctr>=10:
            return True
    else:
        if ctr>0:
            return True
    return False


def notcontains_invalid_terms(phone_text):
    phone_text = phone_text.lower()
    for term in ['aviv', 'hashomer', 'tell', 'tela', 'teli','telu', 'tely', 'tels', 'telm', 'telb', 'teln', 'teleg']:
        if term in phone_text:
            return False
    return True


def get_mod_span(span, occurances):
    inc = 0
    s, e = span
    for o in occurances:
        if s < o < e:
            inc += 1
    e = e + 5*inc
    return s, e


def is_phone_valid(phone_text):
    if has_digit(phone_text) and notcontains_invalid_terms(phone_text):
        return True
    return False


def is_valid(text, start, end, correct_tag, k):

    if k.encode('utf-8') not in text[start:end]:
        #check if the local sequence contains the misclassified text
        return False

    if correct_tag == 'phone':
        #check for valid phone number
        countdigit = len([c for c in text if c.isdigit()])
        if countdigit < 10:
            return False

    return True


def get_actual_s_e(s, e, seq):
    lb_pos = [m.start() for m in re.finditer('%lb', seq)]
    no_lb_text = ' '.join(seq.replace('%lb%', '').split())
    s_id = get_tok_id(no_lb_text, s)
    s_word = no_lb_text.split()[s_id]
    ms1 = re.finditer(re.escape(s_word), no_lb_text)
    pos = {ss.start(): idx for idx, ss in enumerate(ms1)}
    if len(pos)>1:
        # print pos,seq, s_word,s
        closest_key = min(pos.keys(), key=lambda x: abs(x - s))#get closest index match
        r = pos[closest_key]
        ms2 = re.finditer(re.escape(s_word), seq)
        pos2 = {idx: ss.start() for idx, ss in enumerate(ms2)}
        stt = pos2[r]
    else:
        m = re.search(re.escape(s_word), seq)
        stt = m.start()
        # stt = pos.keys()[0]
    actual_s_id = stt
    adjusted_e = actual_s_id - s + e
    _, actual_e_id = get_mod_span((actual_s_id, adjusted_e), lb_pos)
    return actual_s_id, actual_e_id


class Mapper:
    """
    mapper class
    """
    label_dict = dict()
    label_dict['email'] = ['Email']
    label_dict['docAuthor'] = ['Prefix', 'GivenName', 'FamilyName', 'AuthorName', 'Degrees', 'Suffix', 'Particle',
                               'InstitutionalAuthorName', 'Author']
    label_dict['affiliation'] = ['OrgName', 'OrgDivision', 'Affiliation']
    label_dict['address'] = ['Street', 'Country', 'City', 'State', 'Postcode', 'OrgAddress', 'Postbox', 'Address']
    label_dict['phone'] = ['Phone', 'Fax']
    label_dict['note'] = ['ArticleNote']
    label_dict['ptr'] = ['URL']
    label_dict['p'] = ['Dummy']
    label_dict['reference'] = ['Reference']
    rev_map = {v:k for k, vs in label_dict.iteritems() for v in vs}

    def __init__(self, grobid_path, aplus_path):
        self.grobid_path = grobid_path
        self.aplus_path = aplus_path
        self.aplus_dict = dict()
        self.grobid_dict = dict()
        a_file = open_file(self.aplus_path)
        g_file = open_file(self.grobid_path)
        self.grobid_root = self.__parse_grobid_xml(g_file)

        self.aplus_root = ET.fromstring(a_file)

        self.child_parent_map = get_child_parent_mapper(self.grobid_root)
        self.capture_correct_phone()
        self.capture_correct_email()

        self.__parse_xml_aplus()
        self.__parse_xml_grobid()

    def __parse_xml_aplus(self):
        """
        parse aplus xml for authorGroup
        :return:
        """
        for r in self.aplus_root.iter():
            # print r.tag,r.text
            if r.tag == "AuthorGroup":
                authors_affilations = r.iter()
                for a in authors_affilations:
                    for n in a:
                        if n.tag == 'Author':
                            self.__add_author(n)
                        elif n.tag == 'Affiliation':
                            self.__add_affiliation(n)

    def __add_author(self, n):
        for i in n:
            if i.tag == 'AuthorName':
                author = ''
                for j in i:
                    if j.text is not None:
                        author = author + ' ' + j.text
                self.append_to_aplus_dict(author, n.tag)
            elif i.tag == 'Contact':
                for j in i:
                    self.append_to_aplus_dict(j.text, j.tag)

    def __add_affiliation(self, n):
        affiliation = ''
        address = ''
        url = ''
        for i in n:
            if i.tag in ['OrgID', 'OrgDivision', 'OrgName']:
                affiliation = affiliation + ' ' + i.text
            elif i.tag == 'OrgAddress':
                for j in i:
                    address = address + ' ' + j.text
            elif i.tag == 'URL':
                url = i.text
        self.append_to_aplus_dict(affiliation, 'Affiliation')
        self.append_to_aplus_dict(address, 'Address')
        self.append_to_aplus_dict(url, 'URL')
        # print affiliation, ',', address, url

    def append_to_aplus_dict(self, text, tag):
        if len(text) > 0:
            for token in text.split():
                if token not in SPL_CHAR:
                    token = re.sub('tel|phone|url', ' ', token, flags=re.IGNORECASE).strip(' ,;:{}().')
                try:
                    self.aplus_dict[token].add((text, tag))
                except KeyError:
                    self.aplus_dict[token] = set()
                    self.aplus_dict[token].add((text, tag))

    def __parse_xml_grobid(self):
        """
        parse the xml for
        targeted tags
        :return:
        """
        tag_list = ['docAuthor', 'note', 'email', 'phone', 'ptr', 'affiliation', 'address', 'p', 'reference']
        g = self.grobid_root.iter()
        for r in g:
            if r.tag in tag_list:
                for m in r.iter():
                    if m.text is not None:
                        tokens = re.split(r'(?:\(ext\.?|;|\s*)', m.text)
                        for token in tokens:
                            if token != '%lb%':
                                try:
                                    if token not in SPL_CHAR and len(token) > 1:
                                        token = re.sub('tel|phone|url', ' ', token, flags=re.IGNORECASE).strip(' ,;:{}().')
                                    self.grobid_dict[token].append(m)
                                except KeyError:
                                    self.grobid_dict[token] = list()
                                    self.grobid_dict[token].append(m)

    def map_and_edit(self):
        """
        map the tokens and edit the tags
        :return:
        """
        s = ET.tostring(self.grobid_root, encoding='utf-8').replace("%lb%", "<lb/>")
        mis_clas = self.__get_misclassified_examples()
        node_tracker = {}
        nodes_to_handle = {}
        for k in mis_clas:
            for e in mis_clas[k]:
                grobid_tag = e[0].tag
                sim_tags = Mapper.label_dict[grobid_tag]
                # e is set of tuples with complete text and the correct tag
                (start, end, aplus_tag) = self.__get_boundary(e, k)
                if Mapper.rev_map.get(aplus_tag, None) is not None:
                    correct_tag = Mapper.rev_map[aplus_tag]
                else:
                    print 'new tag is found: ' + aplus_tag
                    continue
                if not (start, end) == (None, None):
                    pos_tup = (start, end)
                    try:
                        if pos_tup in node_tracker[e[0]]:
                            continue
                        else:
                            node_tracker[e[0]].append(pos_tup)
                            nodes_to_handle[e[0]].append((pos_tup, correct_tag))
                    except KeyError:
                        node_tracker[e[0]] = [pos_tup]
                        nodes_to_handle[e[0]] = [(pos_tup, correct_tag)]

        self.modify_xml(nodes_to_handle)
        return ET.tostring(self.grobid_root, encoding='utf-8').replace("%lb%", "<lb/>")

    def __get_boundary(self, e, k):
        grobid_text = e[0].text.encode('utf-8')
        matched_tag_tuples = e[1]
        correct_aplus_tag = None
        max_score = 0
        score_matrix, traceback_pos = None, None
        g_text = remove_email_text(grobid_text, matched_tag_tuples)
        for tup in matched_tag_tuples:
            aplus_text = tup[0].strip()
            a_texts = [aplus_text]
            aplus_tag = tup[1]
            if aplus_tag == 'Author':
                poss_variations = get_poss_variations(aplus_text)
                a_texts = poss_variations
            for poss_name in a_texts:
                score_mat, tb_pos, score = create_score_matrix(g_text, poss_name.encode('utf-8'))
                # start, end, match_score = get_match_score(grobid_text, aplus_text)
                if max_score < score:
                    max_score = score
                    score_matrix = score_mat
                    traceback_pos = tb_pos
                    correct_aplus_tag = aplus_tag
        start_char_id = get_start_alignment_id(score_matrix, traceback_pos)
        end_char_id = traceback_pos[0]
        # print g_text[start_char_id:end_char_id]
        s_id = get_tok_id(g_text, start_char_id)
        e_id = get_tok_id(g_text, end_char_id-1, end=True)
        if s_id != 0 and s_id == e_id and aplus_tag == 'Author' and 1 < len(aplus_text.split())<3:
            if g_text.split()[s_id-1][0] == aplus_text.split()[0][0]:
                s_id -= 1
        token_seq = g_text.split()
        correct_tag = Mapper.rev_map[correct_aplus_tag]
        valid = is_valid(g_text, start_char_id, end_char_id, correct_tag, k)
        start = self.__get_left_idx(s_id, token_seq, correct_tag)
        end = self.__get_right_idx(e_id, token_seq, correct_tag)
        # print start, end, correct_tag, grobid_text
        return (start, end, correct_aplus_tag) if valid else (None, None, correct_aplus_tag)

    def __get_misclassified_examples(self):
        mis_clas = {}
        for key in self.grobid_dict:
            if self.aplus_dict.get(key, None) is not None and key not in STOPWORDS and len(key.strip('. ')) > 1:
                # check if the tag matches for each node the token is in
                for node in self.grobid_dict[key]:
                    tags = set()
                    # for tag in self.aplus_dict[key]:
                    for tag in [m[1] for m in self.aplus_dict[key]]:
                        try:
                            tags.add(Mapper.rev_map[tag])
                        except KeyError:
                            print 'new tag is found: '+tag
                    if node.tag not in tags:
                        try:
                            mis_clas[key].append(
                                # (node, list(self.aplus_dict[key])[0]))  # get the correct label # need to handle
                                # (node, [m[1] for m in self.aplus_dict[key]]))
                                (node, self.aplus_dict[key]))
                            # when the token has multiple label
                        except KeyError:
                            mis_clas[key] = list()
                            # mis_clas[key].append((node, [m[1] for m in self.aplus_dict[key]]))
                            mis_clas[key].append((node, self.aplus_dict[key]))
        return mis_clas

    def capture_correct_email(self):
        r = self.grobid_root
        nodes_to_handle_email = self.get_nodes_with_email(r)
        self.modify_xml_for_email(nodes_to_handle_email)

    def capture_correct_phone(self):
        r = self.grobid_root
        nodes_to_handle_phone = self.get_nodes_with_phone(r)
        self.modify_xml_for_phone(nodes_to_handle_phone)
        # print ET.tostring(self.grobid_root, encoding='utf-8').replace("%lb%", "<lb/>")

    def get_nodes_with_email(self, r):
        nodes_to_handle_email = []
        for node in r.iter():
            if node.tag in ['docAuthor', 'note', 'ptr', 'affiliation', 'address', 'p', 'reference', 'phone','idno', 'date','div' ]:
                text = ' '.join(node.text.split())
                lb_pos = [m.start() for m in re.finditer('%lb', text)]
                no_lb_text = ' '.join(text.replace('%lb%', '').split())
                mposs = []
                found = False
                if '@' in no_lb_text:
                    if node.tag == 'div':
                        end = len(no_lb_text)
                        ms = re.finditer('abstract', no_lb_text.lower())
                        for m in ms:
                            end = m.start()
                            break
                        matches = re.finditer(
                            r"(?:(?:email|mail|e-mail)[-: ]*)?(?:[a-z.0-9!#$%&'*+/=?^_`{|}~\-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~\-]+)*|\"(?:[\x01-\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])",
                            no_lb_text[:end], flags=re.IGNORECASE)

                    else:
                        matches = re.finditer(
                            r"(?:(?:email|mail|e-mail)[-: ]*)?(?:[a-z.0-9!#$%&'*+/=?^_`{|}~\-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~\-]+)*|\"(?:[\x01-\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])",
                            no_lb_text, flags=re.IGNORECASE)

                    for match in matches:
                        sp = match.span()
                        mposs.append(match.span())
                        found = True

                    if found == True:
                        nodes_to_handle_email.append((node, mposs))
        return nodes_to_handle_email

    def get_nodes_with_phone(self, r):
        nodes_to_handle_phone = []
        for node in r.iter():
            if node.tag in ['docAuthor', 'note', 'ptr', 'affiliation', 'address', 'p', 'reference', 'email','idno', 'date','div' ]:
                text = ' '.join(node.text.split())
                lb_pos = [m.start() for m in re.finditer('%lb', text)]
                no_lb_text = ' '.join(text.replace('%lb%', '').split())
                mposs = []
                found = False
                if node.tag == 'div':
                    end = len(no_lb_text)
                    ms = re.finditer('abstract', no_lb_text.lower())
                    for m in ms:
                        end = m.start()
                        break
                    matches=re.finditer(r"(?:\+\d{1,2}\s)?\(?\d{1}\)?[\s.\-]\d{3,4}[\s.\-]\d{3,4}\s\d{3,4}[,.]?\s|"
                                        r"(?:(?:\+\d{1,2}\s)?\(?\d{1}\)?[\s.\-]\d{3,4}[\s.\-]\d{3,4}[,]?\s*(?:\(?ext|x[.\d;), ]*)?(?:\s*\(?(?:office|fax|telephone|tele|tel|phone|mobile|pho:|mob:|cell|voice)\)?))|"
                                        r"(?:\+\d{1,2}\s)?\(?\d{3}\)?[\s.\-]\d{3}[\s.\-]\d{4}[,]?\s*(?:\(?ext|x[.\d;), ]*)?(?:\s*\(?(?:office|fax|tel|tele|telephone|phone|mobile|pho:|mob:|cell|voice)\)?)|"
                                        r"(?:(?:telephone|tele|tel|phone|ph:|fax|mobile|mob:|pho:|cell)\s*(?:number)?[\s:.\-–]*[()+\-\d\s–]+(?:[;:,\s]+fax\s*(?:number)?[\s:.\-–]*[()+\-\d\s]+)?)|"
                                        r"(?:(?:\+?\d{1,2}\s)\(?\d{4}\)[\s.\-]\d{4,6})|"
                                        r"(?:(?:\+?\d{1,2}\s)\+?\(?\d{3}\)[\s.\-]\d{4,10})|"
                                        r"(?:(?:\+?\d{1,2}\s)?\+?\(?\d{1,3}\)?[\s.\-]\d{7,10})",
                                      no_lb_text[:end], flags=re.IGNORECASE)
                else:
                    matches = re.finditer(
                        r"(?:\+\d{1,2}\s)?\(?\d{1}\)?[\s.\-]\d{3,4}[\s.\-]\d{3,4}\s\d{3,4}[,.]?\s|"
                        r"(?:(?:\+\d{1,2}\s)?\(?\d{1}\)?[\s.\-]\d{3,4}[\s.\-]\d{3,4}[,]?\s*(?:\(?ext|x[.\d;), ]*)?(?:\s*\(?(?:office|fax|telephone|tele|tel|phone|mobile|pho:|mob:|cell|voice)\)?))|"
                        r"(?:\+\d{1,2}\s)?\(?\d{3}\)?[\s.\-]\d{3}[\s.\-]\d{4}[,]?\s*(?:\(?ext|x[.\d;), ]*)?(?:\s*\(?(?:office|fax|telephone|tele|tel|phone|mobile|pho:|mob:|cell|voice)\)?)|"
                        r"(?:(?:fax)[&.:() /a-zA-Z#]*[\d+() \-–/;,.]+(?:tel[.: \d\-–]*)?(?:\(?ext|x[.\d;), ]*)?)|"
                        r"(?:(?:(?:contact|office|telephone|tele|tel|phone|ph:|fax|pho:|mobile|mob:|cell|voice)[&.:() /a-zA-Z#]*[\d+() \-–/\\;\,.]+)\s*(?:\(?ext|x[.\d;), ]*)?\s*(?:fax\s?(?:number)?[.:–\-]*)?(?:tel\s?(?:number)?[.:–\- ]*)?(?:\(?ext|x[.\d;\), ]*)?\s*[\d\+\(\) \-\–\#\.\]*)|"
                        r"([(tf) ]*(\+\d{1,2}\s)?\(?\d{3}\)?[\s.\-]\d{3}[\s.\-]\d{4}[,]?\s*(?:\(?ext|x[.\d;), ]*)?)",
                        no_lb_text, flags=re.IGNORECASE)

                for match in matches:
                    # print match.group()
                    if is_phone_valid(match.group()):
                        sp = match.span()
                        mposs.append(match.span())
                        found = True

                if found == True:
                    nodes_to_handle_phone.append((node, mposs))
        return nodes_to_handle_phone

    def modify_xml_for_email(self, nodes_to_handle_email):
        """
        """
        for node, mposs in nodes_to_handle_email:
            token_seq = node.text.split()
            text_seq = ' '.join(token_seq)# remove any multiple spaces btw words
            mod_mposs = []
            (s, e) = (mposs[0][0], mposs[0][1])
            running = True
            for i in range(len(mposs) - 1):
                curr = mposs[i]
                fol = mposs[i + 1]
                if curr[1] + 1 != fol[0]:
                    e = curr[1]
                    ms, me = get_actual_s_e(s, e, text_seq)
                    mod_mposs.append((ms, me))
                    s = fol[0]
                    running = False
                else:
                    e = fol[1]
                    running = True

            if running == False:
                ss, ee = mposs[-1]
                ms, me = get_actual_s_e(ss, ee, text_seq)
                mod_mposs.append((ms, me))
            else:
                ms, me = get_actual_s_e(s, e, text_seq)
                mod_mposs.append((ms, me))

            mod_mposs = [(get_tok_id(text_seq, s), get_tok_id (text_seq, e-1, end=True)) for (s, e) in mod_mposs]

            parent = self.child_parent_map[node]
            index = list(parent).index(node)

            tag = node.tag
            attrib = node.attrib
            if tag == 'div' and node.attrib['type'] == 'abstract':
                tag = 'note'
                attrib = {}
            child0 = ET.Element(tag)
            child0.attrib = attrib
            child0.text = ' '.join(token_seq[0:mod_mposs[0][0]])
            if len(child0.text) > 0:
                parent.insert(index, child0)
            i = 0
            for boundary in mod_mposs:
                index += 1
                leftidx, rightidx = boundary
                child = ET.Element('email')
                if rightidx+1 < len(token_seq):
                    if token_seq[rightidx+1] == '%lb%':
                        rightidx = rightidx + 1
                child.text = ' '.join(token_seq[leftidx:rightidx+1])
                parent.insert(index, child)
                if i < len(mod_mposs)-1:
                    leftidx_ahead, rightidx_ahead = mod_mposs[i+1]
                    # check if two consecutive tags are separated by unlabeled tokens
                    if leftidx_ahead > rightidx+1:
                        tag = node.tag
                        attrib = node.attrib
                        if tag == 'div' and node.attrib['type'] == 'abstract':
                            tag = 'note'
                            attrib = {}
                        child = ET.Element(tag)
                        child.attrib = attrib
                        child.text = ' '.join(token_seq[rightidx+1:leftidx_ahead])
                        index += 1
                        parent.insert(index, child)
                i += 1
            num_tags = len(mod_mposs)
            # if last tokens have no new tags
            # label them with existing tags in the grobid file
            # if mod_mposs[-1][1] < len(token_seq)-1:
            if rightidx + 1 < len(token_seq):
                child1 = ET.Element(node.tag)
                child1.attrib = node.attrib
                left = rightidx + 1 # seek next index
                right = len(token_seq)
                child1.text = ' '.join(token_seq[left:right])
                parent.insert(index+1, child1)
            parent.remove(node)
            s = ET.tostring(self.grobid_root, encoding='utf-8').replace("%lb%", "<lb/>")
            self.child_parent_map = get_child_parent_mapper(self.grobid_root)


    def modify_xml_for_phone(self, nodes_to_handle_phone):
        """
        """
        for node, mposs in nodes_to_handle_phone:
            token_seq = node.text.split()
            text_seq = ' '.join(token_seq)# remove any multiple spaces btw words
            mod_mposs = []
            (s, e) = (mposs[0][0], mposs[0][1])
            running = True
            for i in range(len(mposs) - 1):
                curr = mposs[i]
                fol = mposs[i + 1]
                # if curr[1] + 1 != fol[0]:
                if curr[1] + 1 < fol[0]:
                    e = curr[1]
                    ms, me = get_actual_s_e(s, e, text_seq)
                    mod_mposs.append((ms, me))
                    s = fol[0]
                    running = False
                else:
                    e = fol[1]
                    running = True

            if running == False:
                ss, ee = mposs[-1]
                ms, me = get_actual_s_e(ss, ee, text_seq)
                mod_mposs.append((ms, me))
            else:
                ms, me = get_actual_s_e(s, e, text_seq)
                mod_mposs.append((ms, me))

            mod_mposs_ids = [(get_tok_id(text_seq, s), get_tok_id (text_seq, e-1, end=True)) for (s, e) in mod_mposs]

            parent = self.child_parent_map[node]
            index = list(parent).index(node)
            tag = node.tag
            attrib = node.attrib
            if tag == 'div' and node.attrib['type'] == 'abstract':
                tag = 'note'
                attrib = {}
            child0 = ET.Element(tag)
            child0.attrib = attrib
            child0.text = ' '.join(token_seq[0:mod_mposs_ids[0][0]])
            if len(child0.text) > 0:
                parent.insert(index, child0)
            i = 0
            for boundary in mod_mposs_ids:
                index += 1
                leftidx, rightidx = boundary
                child = ET.Element('phone')
                if rightidx+1 < len(token_seq):
                    if token_seq[rightidx+1] == '%lb%':
                        rightidx = rightidx + 1
                child.text = ' '.join(token_seq[leftidx:rightidx+1])
                parent.insert(index, child)
                if i < len(mod_mposs_ids)-1:
                    leftidx_ahead, rightidx_ahead = mod_mposs_ids[i+1]
                    # check if two consecutive tags are separated by unlabeled tokens
                    if leftidx_ahead > rightidx+1:
                        tag = node.tag
                        attrib = node.attrib
                        if tag == 'div' and node.attrib['type'] == 'abstract':
                            tag = 'note'
                            attrib = {}
                        child = ET.Element(tag)
                        child.attrib = attrib
                        child.text = ' '.join(token_seq[rightidx+1:leftidx_ahead])
                        index += 1
                        parent.insert(index, child)
                i += 1
            num_tags = len(mod_mposs_ids)
            # if last tokens have no new tags
            # label them with existing tags in the grobid file
            # if mod_mposs[-1][1] < len(token_seq)-1:
            if rightidx + 1 < len(token_seq):
                child1 = ET.Element(node.tag)
                child1.attrib = node.attrib
                left = rightidx + 1 # seek next index
                right = len(token_seq)
                child1.text = ' '.join(token_seq[left:right])
                parent.insert(index+1, child1)
            parent.remove(node)

            s= ET.tostring(self.grobid_root, encoding='utf-8').replace("%lb%", "<lb/>")
            self.child_parent_map = get_child_parent_mapper(self.grobid_root)

    def modify_xml(self, nodes_to_handle):
        """
        build modifications and modify the grobid xml
        :param nodes_to_handle:
        :return:
        """
        for node in nodes_to_handle:
            parent = self.child_parent_map[node]
            token_seq = node.text.split()
            # index = parent.getiterator().index(node)
            index = list(parent).index(node)
            # sort the positions
            sorted_tups = sorted(nodes_to_handle[node], key=lambda x: x[0], reverse=False)
            cleaned_list = self.__clean_up(sorted_tups)
            if len(cleaned_list) > 0:
                sorted_positions, tags = cleaned_list
            else:
                continue
            # sorted_positions, tags = zip(*sorted_tups)
            child0 = ET.Element(node.tag)
            child0.attrib = node.attrib
            child0.text = ' '.join(token_seq[0:sorted_positions[0][0]])
            if len(child0.text) > 0:
                parent.insert(index, child0)
            i = 0
            for boundary in sorted_positions:
                index += 1
                leftidx, rightidx = boundary
                child = ET.Element(tags[i])
                child.text = ' '.join(token_seq[leftidx:rightidx+1])
                parent.insert(index, child)
                if i < len(sorted_positions)-1:
                    leftidx_ahead, rightidx_ahead = sorted_positions[i+1]
                    # check if two consecutive tags are separated by unlabeled tokens
                    if leftidx_ahead > rightidx+1:
                        child = ET.Element(node.tag)
                        child.attrib = node.attrib
                        child.text = ' '.join(token_seq[rightidx+1:leftidx_ahead])
                        index += 1
                        parent.insert(index, child)
                i += 1
            num_tags = len(sorted_positions)
            # if last tokens have no new tags
            # label them with existing tags in the grobid file
            if sorted_positions[num_tags-1][1] < len(token_seq)-1:
                child1 = ET.Element(node.tag)
                child1.attrib = node.attrib
                left = sorted_positions[num_tags-1][1] + 1 # seek next index
                right = len(token_seq)
                child1.text = ' '.join(token_seq[left:right])
                parent.insert(index+1, child1)
            parent.remove(node)

    def __clean_up(self, sorted_tups):
        """
        exclude items already contained or tagged
        :param sorted_tups:
        :return:
        """
        sorted_positions, tags = zip(*sorted_tups)
        crop_list = set()
        for i in range(len(sorted_positions)):
            # ignore tag(other than email,phone,url) of single token
            if sorted_positions[i][0] == sorted_positions[i][1] and tags[i] not in ['affiliation', 'address', 'email', 'phone', 'ptr', 'docAuthor']:
                crop_list.add(i)
                continue
            for j in range(i+1, len(sorted_positions)):
                if sorted_positions[i][1] >= sorted_positions[j][1]:
                    crop_list.add(j)
        cropped_tup = zip(*(t2 for t1, t2 in enumerate(sorted_tups) if t1 not in crop_list))
        return cropped_tup

    # @staticmethod
    # def get_indexes(p, t):
    #     dist, (start, end) = edit_distance(p, t)
    #     return dist, (start, end)

    @staticmethod
    def __parse_grobid_xml(gf):
        return Mapper.__handle_line_break(gf)

    @staticmethod
    def __handle_line_break(gf):
        """
        takes the grobid generated file
        handle selfclosing lb
        form mapping for token and the node
        """
        str_data = gf.replace('<lb/>', ' %lb% ')
        grobid_xml = Mapper.__parse_xml(str_data)
        return grobid_xml

    @staticmethod
    def __parse_xml(str_data):
        rootG = ET.fromstring(str_data)
        return rootG

    def __get_right_idx(self, idx, token_seq, correct_tag):
        """
        seek for the right hand side boundary
        :param idx:
        :param token_seq:
        :param correct_tag:
        :return:
        """
        right = idx
        if idx+1 < len(token_seq):
            tok = token_seq[idx+1]
            if tok == '%lb%':
                right += 1
        return right

    def __get_left_idx(self, idx, token_seq, correct_tag):
        left = idx
        for i in range(idx - 1, -1, -1):
            tok = token_seq[i]
            # if the token is in same context
            # at the moment handle only email author and phone
            if correct_tag in ['email', 'docAuthor', 'phone']:
                preceding_text = tok.lower().strip(':-)(\/;.,')
                context_words = [word.lower() for word in contextual_words[correct_tag]]
                if preceding_text in context_words:
                    left -= 1
                    continue
                return left
        return left

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('-pG', default=config.path_to_grobid)
    parser.add_argument('-pA', default=config.path_to_aplus)
    parser.add_argument('-pO', default=config.out_path)

    args = parser.parse_args()
    # get the grobid and aplusaplus files
    in_files = [(os.path.join(args.pG, g), os.path.join(args.pA, a)) for g in os.listdir(args.pG)\
                if g.endswith('.header.tei.xml') for a in os.listdir(args.pA) if a.split('.')[0] in g]
    # in_files = [('/home/aman/data/training/mapper3.0/Grobid_data/10597_2016_15.header.tei.xml', '/home/aman/data/training/input_data_xml/10597_2016_15.xml')]
    for files_ in in_files:
        grobid_file, reference_file = files_[0], files_[1]
        try:
            mapper = Mapper(grobid_file, reference_file)
            # print files_[0]
            # copy(os.path.join(path_to_grobid, files_[0]), "/home/aman/data/training/grobid_files")
            out_xml = mapper.map_and_edit()
            with codecs.open(os.path.join(args.pO, os.path.basename(files_[0])), 'w', encoding='utf-8') as f:
                f.write('<?xml version="1.0" ?>\n')
                f.write(out_xml.decode(encoding='utf-8'))
        except ET.ParseError:
            print grobid_file


