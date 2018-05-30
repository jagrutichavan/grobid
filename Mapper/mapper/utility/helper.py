import xml.etree.ElementTree as ET
import codecs


def get_child_parent_mapper(root):
    tree = ET.ElementTree(root)
    c_p_map = {c: p for p in tree.iter() for c in p}
    return c_p_map


def open_file(path):
    with codecs.open(path, encoding='utf8') as f:
        data = f.read()
        return data.encode('utf-8')


def parse_grobid_xml(gf):
    return handle_line_break(gf)


def handle_line_break(gf):
    """
    takes the grobid generated file
    handle selfclosing lb
    form mapping for token and the node
    """
    str_data = gf.replace('<lb/>', ' %%lb%% ')
    grobid_xml = parse_xml(str_data)
    return grobid_xml


def parse_xml(str_data):
    root_g = ET.fromstring(str_data)
    return root_g