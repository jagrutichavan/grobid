import xml.etree.cElementTree as ET
import entity.matchtagValue as matchTagValue
import entity.affiliationList as affList
import entity.tagValue as tagValue

class affiliationTei:
    def __init__(self, affTeipath):
        self.path = affTeipath
        self.xpath = '{0}teiHeader/{0}fileDesc/{0}sourceDesc/{0}biblStruct/{0}analytic/{0}author'
        self.namespace = ''
        self.affListWithSenetence = []
        self.affWordsList = []
        self.affs = []

    def getAffList(self):
        allAuthor = self.loadXml()
        for auth in allAuthor:
            allAff = auth.findall('{0}affiliation'.format(self.namespace))
            for aff in allAff:
                self.affs.append(self.getAffTextSepratedwithSpace(aff))
                # self.affListWithSenetence.append(affText)


    def loadXml(self):
         tree = ET.parse(self.path)
         self.namespace = '{' + tree.getroot().tag[1:].split("}")[0] + '}'
         return tree.findall(self.xpath.format(self.namespace))

    def getAffTextSepratedwithSpace(self,aff):
        sentence = ''
        # tagValueList = []
        affs = affList.affiliationList()
        for elem in aff:
            if('address' in elem.tag):
                # address = aff.findall('{0}address'.format(self.namespace))
                # if(address is not None and len(address) > 0):
                #     for add in address:
                #         for addelm in add:
                #             sentence += addelm.text + ' '
                #             tagValueList.append(tagValue.tagValue(addelm.tag, addelm.text))
                #             # sentence += addelm.text + ' '
                #             for txt in addelm.text.split(' '):
                #                 tagValueList.append(tagValue.tagValue(addelm.tag, txt))
                continue
            else:
                sentence += elem.text + ' '
                affs.tagValueList.append(matchTagValue.matchTagValue(elem.tag, elem.text))
                # sentence += elem.text + ' '
                for txt in elem.text.split(' '):
                    affs.wordTagValue.append(tagValue.tagValue(elem.tag, txt))

        affs.sentence = sentence

        return affs



if __name__ == '__main__':
    aff = affiliationTei('/Users/gpm1181/Documents/ws/grobid-output/122_2017_2860.affiliation.tei.xml')
    aff.self.teiPath()