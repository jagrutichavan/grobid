import docx
import os

label_dict = dict()
label_dict['email'] = ['Email','EmailId']
label_dict['docAuthor'] = ['Initials','Prefix', 'GivenName', 'FamilyName', 'AuthorName', 'Degrees', 'Suffix', 'Particle',
                           'InstitutionalAuthorName']
label_dict['affiliation'] = ['OrgName', 'OrgDivision']
label_dict['address'] = ['Street', 'Country', 'City', 'State', 'Postcode', 'OrgAddress', 'Postbox']
label_dict['phone'] = ['Phone', 'Fax']
label_dict['other'] = ['O']

rev_map = {v: k for k, vs in label_dict.iteritems() for v in vs}

str = """URL = "C0C0FF";
Author_Fig = "FF80FF";
Role = "7FFA54";
Particle = "FFFF80";
Prefix = "FF8633";
Suffix = "FFA86D";
GivenName = "DDDDDD";
FamilyName = "BCBCBC";
MiddleName = "9C9C9C";
Degrees = "00C400";
Biography = "FFC0FF";
AuthorFig = "FF80FF"; 
EmailId = "FFCC00";
OrgDivision = "8080FF";
OrgName = "00FF99";
Street = "33CCCC";
Postcode = "C6C6C6";
City = "66FFFF";
Country = "00A5E0";
EdFamilyName = "FF95CA";
Initials = "DDDDDD";
Year = "66FF66";
ChapterTitle = "FF9933";
EdInitials = "FFD1E8";
BookTitle = "FFD9B3";
PublisherName = "FFFF49";
PublisherLocation = "C0FFC0";
ArticleTitle = "CCCCFF";
JournalTitle = "CCFF99";
VolumeID = "FFCC66";
FirstPage = "D279FF";
EditionNumber = "9999FF";
EdGivenName = "FFD1E8";
State = "00CC99";
IssueID = "C8BE84";
InstitutionalAuthorName = "5B96A2";
BibInstitutionalEditorName = "F9A88F";
PostBox = "F7D599";
FAX = "FEC0CC";
Phone = "91C8FF";
ISN = "A17189";
ISBN = "C8EBFC";
DOI = "CFBFB1";
NoColor = "FFFFFF";
BibComments = "C09200";
O = "NOCOLOR";
"""

color_map = {}
for line in str.split(';'):
    tokens = line.split('=')
    if len(tokens)>1:
        color_map[tokens[1].strip('" ')] = tokens[0].strip('\n ')


URL = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
in_file = "/home/aman/data/data/GROBIDFILES/DATA-01-03-2016-TO-27-01-2017/10854_2016_6194_Article/10854_2016_6194.docm"
in_f = "/home/aman/data/data/GROBIDFILES/DATA-01-03-2016-TO-27-01-2017/10854_2016_6194_Article/10854_2016_6194.docm"
dir_path = "/home/aman/data/data/GROBIDFILES/DATA-01-03-2016-TO-27-01-2017/"


class Entity(object):
    def __init__(self, text=None, color=None):
        self.__text = text
        self.__color = color

    @property
    def t(self):
        return self.__text

    @t.setter
    def t(self, text):
        self.__text = text

    @property
    def c(self):
        return self.__color

    @c.setter
    def c(self, color):
        self.__color = color

    @property
    def pt(self):
        if self.c is not None:
            return rev_map[color_map[self.c]]
        else:
            return 'other'


def get_color_run(run):
    run_child = run.element.rPr
    if run_child is not None:
        s = run_child.xpath('w:shd')
        if len(s) > 0:
            e = s[0]
            color = e.attrib[URL + 'fill']
            return color
    return 'NOCOLOR'


def sep_entity(run, prev_col):
    """
    based on color and sub-level tag
    """
    prev_tag = color_map[prev_col]
    run_child = run.element.rPr
    if run_child is not None:
        color = get_color_run(run)
        if prev_col == color and prev_col != 'NOCOLOR':
            return False
    return True

def first_page_ends(run):
    if run.element.xpath('w:lastRenderedPageBreak'):
        return True
    return False


def get_annotation(in_file):
    labeled_es = []
    document = docx.Document(in_file)
    prev_color = 'NOCOLOR'
    for para in document.paragraphs:
        for run in para.runs:
            if first_page_ends(run):
                return labeled_es
            if sep_entity(run, prev_color):
                e = Entity()
                e.t = run.text
                color = get_color_run(run)
                e.c = color
                labeled_es.append(e)
                prev_color = color

            else:
                # continuos entity
                e.t = e.t + run.text
    return labeled_es

def drive():
    i = 0
    for dir_ in os.listdir(dir_path):
        path = os.path.join(dir_path,dir_)
        if os.path.isdir(path):
            for f in os.listdir(path):
                if f.endswith('.docm'):
                    if i >= 1:
                        return
                    header_data = []
                    cursor_on_docm = []
                    file_path = os.path.join(path,in_file)
                    file_path = "/home/aman/data/data/GROBIDFILES/DATA-01-03-2016-TO-27-01-2017/128_2016_1942_Article/128_2016_1942.docm"
                    labeled_es = get_annotation(file_path)
                    prev_label = 'ABCD'
                    text_list = []
                    for e in labeled_es:
                        print e.t, e.pt, e.c
                    # for e in labeled_es:
                    #     START = True
                    #     for tok in e.t.split():
                    #         if START or e.c == 'NOCOLOR':
                    #             print tok, 'B-'+rev_map[color_map[e.c]]
                    #             START = False
                    #         else:
                    #             print tok, 'I-'+rev_map[color_map[e.c]]
                    # print '\n'
                    i += 1

if __name__ == '__main__':
    drive()

