import codecs
import xml.etree.ElementTree as ET

class helper:
    def open_file(self,path):
        with codecs.open(path, encoding='utf8') as f:
            data = f.read()
            return data.encode('utf-8')

    def get_child_parent_mapper(self,root):
        ET.register_namespace('', 'http://www.tei-c.org/ns/1.0')
        tree = ET.ElementTree(root)
        c_p_map = {c: p for p in tree.iter() for c in p}
        return c_p_map

