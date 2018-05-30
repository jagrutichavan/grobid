# -*- coding: utf-8 -*-
import ConfigParser
import argparse
import codecs
import xml.etree.ElementTree as ET

import os

import helper as Helper
import re
from LocalAlignment import create_score_matrix, get_start_alignment_id

class tagMapper:
    """
    mapper class
    """
    label_dict = dict()
    label_dict['orgName'] = ['OrgDivision','OrgName']
    label_dict['addrLine'] = ['Street']
    label_dict['postBox'] = ['Postbox']
    label_dict['postCode'] = ['Postcode']
    label_dict['settlement'] = ['City']
    label_dict['region'] = ['State']
    label_dict['country'] = ['Country']
    rev_map = {v:k for k, vs in label_dict.iteritems() for v in vs}


class mapper:

    def __init__(self,grobid_path, aplus_path):
        self.STOPWORDS = """
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
        self.SPL_CHAR = [',', ';', ':', 'tel', '(', ')']
        self.EMAIL_REGEX = re.compile(r"[^@]+@[^@]+\.[^@]+")
        self.contextual_words = {
            'email': ['corresponding', 'email', 'e-mail', 'address', 'mail', 'addresses'],
            'phone': ['telephone', 'ph', 'tel', 'tele', 'numbers', 'number', 'tel/fax', 'tel:/fax', 'cellphone',
                      'mobile', \
                      'no', 'and', 'tel', 'phone', 'tel./fax'],
            'docAuthor': ['corresponding', 'authors', 'authors', "authors'", 'to', 'correspondence', 'information',
                          '&amp;', 'dr.', '-dr.', 'phd'], \
            'address': ['and', '&amp;'],
            'affiliation': ['institutional', 'affiliation'],
            'ptr': ['url']
        }
        self.Email_dict = {}
        self.helperObj = Helper.helper()
        self.tagMapper = tagMapper()
        self.grobid_path = grobid_path
        self.aplus_path = aplus_path
        self.aplus_dict = dict()
        self.grobid_dict = dict()
        a_file = self.helperObj.open_file(self.aplus_path)
        g_file = self.helperObj.open_file(self.grobid_path)
        self.grobid_root = self.__parse_grobid_xml(g_file)
        self.namespace = '{' + self.grobid_root.tag[1:].split("}")[0] + '}'
        self.aplus_root = ET.fromstring(a_file)

        self.child_parent_map = self.helperObj.get_child_parent_mapper(self.grobid_root)

        self.__parse_xml_aplus()
        self.__parse_xml_grobid()


    def __parse_grobid_xml(self,gf):
        return self.__handle_line_break(gf)

    def __handle_line_break(self,gf):
        str_data = gf.replace('<lb/>', ' %lb% ')
        grobid_xml = self.__parse_xml(str_data)
        return grobid_xml

    def __parse_xml(self,str_data):
        rootG = ET.fromstring(str_data)
        return rootG

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
                        if n.tag == 'Affiliation':
                            self.__add_affiliation(n)

    def __add_affiliation(self, n):
        affiliation = ''
        address = ''
        url = ''
        for i in n:
            if (i.tag != 'OrgAddress'):
                self.append_to_aplus_dict(i.text, i.tag)
            else:
                for j in i:
                    self.append_to_aplus_dict(j.text, j.tag)
            # if i.tag in ['OrgID', 'OrgDivision', 'OrgName']:
            #     affiliation = affiliation + ' ' + i.text
            # elif i.tag == 'OrgAddress':
            #     for j in i:
            #         address = address + ' ' + j.text
            # elif i.tag == 'URL':
            #     url = i.text
        # self.append_to_aplus_dict(affiliation, 'Affiliation')
        # self.append_to_aplus_dict(address, 'Address')
        # self.append_to_aplus_dict(url, 'URL')
        print ''

    def append_to_aplus_dict(self, text, tag):
        if len(text) > 0:
            for token in text.split():
                if token not in self.SPL_CHAR:
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
        tag_list = ['{0}author'.format(self.namespace),'{0}affiliation'.format(self.namespace),'{0}orgName'.format(self.namespace),'{0}address'.format(self.namespace),
                    '{0}addrLine'.format(self.namespace),'{0}postBox'.format(self.namespace), '{0}settlement'.format(self.namespace),'{0}postCode'.format(self.namespace),
                    '{0}region'.format(self.namespace),'{0}country'.format(self.namespace)]
        g = self.grobid_root.iter()
        for r in g:
            if r.tag in tag_list:
                allAff = r.findall('{0}affiliation'.format(self.namespace))
                for aff in allAff:
                    for m in aff:
                        if (m.tag == '{0}address'.format(self.namespace)):
                            for n in m:
                                if n.text is not None:
                                    tokens = re.split(r'(?:\(ext\.?|;|\s*)', n.text)
                                    for token in tokens:
                                        if token != '%lb%':
                                            try:
                                                if token not in self.SPL_CHAR and len(token) > 1:
                                                    token = re.sub('tel|phone|url', ' ', token,
                                                                   flags=re.IGNORECASE).strip(' ,;:{}().')
                                                self.grobid_dict[token].append(n)
                                            except KeyError:
                                                self.grobid_dict[token] = list()
                                                self.grobid_dict[token].append(n)
                        elif m.text is not None:
                            tokens = re.split(r'(?:\(ext\.?|;|\s*)', m.text)
                            for token in tokens:
                                if token != '%lb%':
                                    try:
                                        if token not in self.SPL_CHAR and len(token) > 1:
                                            token = re.sub('tel|phone|url', ' ', token, flags=re.IGNORECASE).strip(' ,;:{}().')
                                        self.grobid_dict[token].append(m)
                                    except KeyError:
                                        self.grobid_dict[token] = list()
                                        self.grobid_dict[token].append(m)

    def get_misclassified_examples(self):
        mis_clas = {}
        for key in self.grobid_dict:
            if self.aplus_dict.get(key, None) is not None and key not in self.STOPWORDS and len(key.strip('. ')) > 1:
                for node in self.grobid_dict[key]:
                    tags = set()
                    for tag in [m[1] for m in self.aplus_dict[key]]:
                        try:
                            tags.add(self.tagMapper.rev_map[tag])
                        except KeyError:
                            print 'new tag is found: '+tag
                    if node.tag not in tags:
                        try:
                            mis_clas[key].append(
                                (node, self.aplus_dict[key]))
                        except KeyError:
                            mis_clas[key] = list()
                            mis_clas[key].append((node, self.aplus_dict[key]))
        return mis_clas

    def getNodeOfAplusAndAffTag(self, mis_clas):
        node_tracker = {}
        nodes_to_handle = {}
        for k in mis_clas:
            for e in mis_clas[k]:
                # grobid_tag = e[0].tag
                # sim_tags = tagMapper.label_dict[grobid_tag]
                (start, end, aplus_tag) = self.__get_boundary(e, k)
                if tagMapper.rev_map.get(aplus_tag, None) is not None:
                    if (aplus_tag == 'OrgDivision'):
                        correct_tag = 'orgName'
                    elif (aplus_tag == 'OrgName'):
                        correct_tag = 'orgName'
                    else:
                        correct_tag = tagMapper.rev_map[aplus_tag]

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
                            nodes_to_handle[e[0]].append((pos_tup, correct_tag,aplus_tag))
                    except KeyError:
                        node_tracker[e[0]] = [pos_tup]
                        nodes_to_handle[e[0]] = [(pos_tup, correct_tag, aplus_tag)]

        return nodes_to_handle

    def __get_boundary(self, e, k):
        grobid_text = e[0].text.encode('utf-8')
        matched_tag_tuples = e[1]
        correct_aplus_tag = None
        max_score = 0
        score_matrix, traceback_pos = None, None
        g_text = self.remove_email_text(grobid_text, matched_tag_tuples)
        for tup in matched_tag_tuples:
            aplus_text = tup[0].strip()
            a_texts = [aplus_text]
            aplus_tag = tup[1]
            if aplus_tag == 'Author':
                poss_variations = self.get_poss_variations(aplus_text)
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
        s_id = self.get_tok_id(g_text, start_char_id)
        e_id = self.get_tok_id(g_text, end_char_id - 1, end=True)
        if s_id != 0 and s_id == e_id and aplus_tag == 'Author' and 1 < len(aplus_text.split()) < 3:
            if g_text.split()[s_id - 1][0] == aplus_text.split()[0][0]:
                s_id -= 1
        token_seq = g_text.split()
        correct_tag = self.tagMapper.rev_map[correct_aplus_tag]
        valid = self.is_valid(g_text, start_char_id, end_char_id, correct_tag, k)
        start = self.__get_left_idx(s_id, token_seq, correct_tag)
        end = self.__get_right_idx(e_id, token_seq, correct_tag)
        # print start, end, correct_tag, grobid_text
        return (start, end, correct_aplus_tag) if valid else (None, None, correct_aplus_tag)

    def remove_email_text(self,text, tag_tuples):
        tags = [m[1] for m in tag_tuples]
        token_seq = text.split()
        if 'email' not in [self.tagMapper.rev_map[tag] for tag in tags]:
            for i, token in enumerate(token_seq):
                if self.EMAIL_REGEX.match(token):
                    new_token = 'electronic_mail' + str(i)
                    token_seq[i] = new_token
                    self.Email_dict[new_token] = token
        return ' '.join(token_seq)

    def get_poss_variations(self,name_text):
        name_tokens = name_text.split()
        if len(name_tokens) > 1:
            shortened_name = ' '.join([tok[0].upper() + '.' for tok in name_tokens[:-1]]) + ' ' + name_tokens[-1]
            if name_text != shortened_name:
                return [name_text, shortened_name]
        return [name_text]

    def get_tok_id(self,seq, cid, end=False):
        """
        assuming tokens are separated by single space
        :param seq target sequence:
        :param cid last macthed character:
        :return number of spaces (last matched token id):
        """
        nb = 0
        c = ''
        for c in seq[:cid + 1]:
            if c == ' ':
                nb += 1
        if end == True and (c == ' ' or c == '('):
            nb -= 1
        return nb

    def is_valid(self,text, start, end, correct_tag, k):

        if k.encode('utf-8') not in text[start:end]:
            # check if the local sequence contains the misclassified text
            return False

        if correct_tag == 'phone':
            # check for valid phone number
            countdigit = len([c for c in text if c.isdigit()])
            if countdigit < 10:
                return False

        return True

    def __get_left_idx(self, idx, token_seq, correct_tag):
        left = idx
        for i in range(idx - 1, -1, -1):
            tok = token_seq[i]
            # if the token is in same context
            # at the moment handle only email author and phone
            if correct_tag in ['email', 'docAuthor', 'phone']:
                preceding_text = tok.lower().strip(':-)(\/;.,')
                context_words = [word.lower() for word in self.contextual_words[correct_tag]]
                if preceding_text in context_words:
                    left -= 1
                    continue
                return left
        return left

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
                sorted_positions, tags, aplusTag = cleaned_list
            else:
                continue
            # sorted_positions, tags = zip(*sorted_tups)
            child0 = ET.Element(node.tag)
            aplusTag = cleaned_list[2][0]
            nodeAttr = {}
            if(aplusTag == 'OrgDivision' and node.tag in ['orgName']):
                node.attrib['type'] = 'department'
                nodeAttr = node.attrib
            elif(aplusTag == 'OrgName' and node.tag in ['orgName']):
                node.attrib['type'] = 'institution'
                nodeAttr = node.attrib


            child0.attrib = nodeAttr
            child0.text = ' '.join(token_seq[0:sorted_positions[0][0]])
            if len(child0.text) > 0:
                parent.insert(index, child0)
            i = 0
            for boundary in sorted_positions:
                index += 1
                leftidx, rightidx = boundary
                child = ET.Element(tags[i])
                child.text = ' '.join(token_seq[leftidx:rightidx+1])
                if(child.tag not in ['country','address','postCode','settlement','addrLine','region','postBox']):
                    child.attrib = node.attrib
                if(i == len(sorted_positions)-1):
                    child.tail = node.tail
                parent.insert(index, child)
                if i < len(sorted_positions)-1:
                    leftidx_ahead, rightidx_ahead = sorted_positions[i+1]
                    # check if two consecutive tags are separated by unlabeled tokens
                    if leftidx_ahead > rightidx+1:
                        child = ET.Element(node.tag)
                        child.attrib = node.attrib
                        child.text = ' '.join(token_seq[rightidx+1:leftidx_ahead])
                        child.tail = node.tail
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
        sorted_positions, tags, aplusTag = zip(*sorted_tups)
        crop_list = set()
        for i in range(len(sorted_positions)):
            # ignore tag(other than email,phone,url) of single token
            if sorted_positions[i][0] == sorted_positions[i][1] and tags[i] not in ['country','address','postCode','settlement','addrLine','region','postBox']:
                crop_list.add(i)
                continue
            for j in range(i+1, len(sorted_positions)):
                if sorted_positions[i][1] >= sorted_positions[j][1]:
                    crop_list.add(j)
        cropped_tup = zip(*(t2 for t1, t2 in enumerate(sorted_tups) if t1 not in crop_list))
        return cropped_tup


if __name__ == '__main__':

    # obj = mapper('/Users/gpm1181/Documents/ws/grobid-output__oo/128_2016_1929.affiliation.tei.xml','/Users/gpm1181/Documents/ws/a-plus/128_2016_1929.xml');
    # print obj.grobid_root
    # mis_clas = obj.get_misclassified_examples()
    # nodes = obj.getNodeOfAplusAndAffTag(mis_clas)
    # obj.modify_xml(nodes)
    # print '------------------------'
    # print obj.grobid_root
    # obj.grobid_root.attrib['xmlns:xlink'] = 'http://www.w3.org/1999/xlink'
    # obj.grobid_root.attrib['xmlns:mml'] = 'http://www.w3.org/1998/Math/MathML'
    # print ET.tostring(obj.grobid_root, encoding='utf-8',method="xml").replace("%lb%", "<lb/>")
    # print ''

    print('.............Mapping Start...........')

    config = ConfigParser.ConfigParser()
    config.readfp(open('../affiliationBuild/config/config.txt'))
    path_to_grobid = config.get('affiliationMapper', 'path_to_grobid')
    path_to_aplus = config.get('affiliationMapper', 'path_to_aplus')
    path_to_pdf = config.get('affiliationMapper', 'pdf_path')
    out_path = config.get('affiliationMapper', 'out_path')
    lxml_out_path = config.get('affiliationMapper', 'lxml_out_path')
    pdf2xml_path = config.get('affiliationMapper', 'pdf2xml_path')

    parser = argparse.ArgumentParser()
    parser.add_argument('-pG', default=path_to_grobid)
    parser.add_argument('-pA', default=path_to_aplus)
    parser.add_argument('-pO', default=out_path)
    args = parser.parse_args()
    in_files = [(os.path.join(args.pG, g), os.path.join(args.pA, a),) for g in os.listdir(args.pG) \
                if g.endswith('.affiliation.tei.xml') for a in os.listdir(args.pA) if (not a.endswith('.DS_Store')) if
                a.split('.')[0] in g]
    errorFiles = ''
    errorCount = 0
    totalFiles = 0
    for files_ in in_files:
        try:
            print('....................File Start...................' + files_[0])
            totalFiles += 1

            obj = mapper(files_[0],files_[1])
            mis_clas = obj.get_misclassified_examples()
            nodes = obj.getNodeOfAplusAndAffTag(mis_clas)
            obj.modify_xml(nodes)
            obj.grobid_root.attrib['xmlns:xlink'] = 'http://www.w3.org/1999/xlink'
            obj.grobid_root.attrib['xmlns:mml'] = 'http://www.w3.org/1998/Math/MathML'
            out_xml = ET.tostring(obj.grobid_root, encoding='utf-8',method="xml").replace("%lb%", "<lb/>")

            with codecs.open(os.path.join(args.pO, os.path.basename(files_[0])), 'w', encoding='utf-8') as f:
                f.write('<?xml version="1.0" ?>\n')
                f.write(out_xml.decode(encoding='utf-8'))

        except Exception, e:
            errorFiles += files_[0] + '\n'
            errorCount += 1
            print('....................Error In...................' + files_[0])
            print('....................Error Message................... ' + str(e.message))

    print('.............Mapping Done...........')
    print('%%%%%%%%%%%%%%%Total Files %%%%%%%%%%%%' + str(totalFiles))
    print('%%%%%%%%%%%%%%%Total Error Files %%%%%%%%%%%%' + str(errorCount))