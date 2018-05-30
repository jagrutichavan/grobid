import entity.tagValue as tagValue
import xml.etree.cElementTree as ET
import entity.affiliationList as affList

class aplus:
    def __init__(self,aplusPath):
        self.xpath = 'ArticleHeader/AuthorGroup/Affiliation'
        self.affListWithSenetence = []
        self.path = aplusPath

    def getAffList(self):
        allAff = self.loadXml()
        for aff in allAff:
            self.affListWithSenetence.append(self.getAffTextSepratedwithSpace(aff))

    def loadXml(self):
         tree = ET.parse(self.path)
         return tree.findall(self.xpath)


    def getAffTextSepratedwithSpace(self,aff):
        sentence = ''
        tagValueList = []

        for elem in aff:
            if('OrgAddress' in elem.tag):
                # address = aff.findall('OrgAddress')
                # if(address is not None and len(address) > 0):
                #     for add in address:
                #         for addelm in add:
                #             sentence += addelm.text + ' '
                #             tagValueList.append(tagValue.tagValue(addelm.tag, addelm.text))
                #             for txt in addelm.text.split(' '):
                #                 tagValueList.append(tagValue.tagValue(addelm.tag, txt))
                continue
            else:
                sentence += elem.text + ' '
                tagValueList.append(tagValue.tagValue(elem.tag, elem.text))
                # for txt in elem.text.split(' '):
                #     tagValueList.append(tagValue.tagValue(elem.tag, txt))

        return affList.affiliationList(sentence.strip(), tagValueList)

if __name__ == '__main__':
    aplusHelper  = aplus('/Users/gpm1181/Documents/ws/a-plus/122_2017_2860.xml');
    # aplusHelper.loadXml()
    aplusHelper.getAffList()



