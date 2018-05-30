import affiliationTei as TEI
import aplus as APLUS


class utility:
    def __init__(self,teiPath='',aplusPath=''):
        self.teiPath = teiPath
        self.aplusPath = aplusPath
        self.teiAffList = []
        self.aplusAffList = []

    def iterateAff(self):

        teiAffObj = TEI.affiliationTei(self.teiPath)
        teiAffObj.getAffList()
        self.teiAffList = teiAffObj.affs


        aplusObj = APLUS.aplus(self.aplusPath)
        aplusObj.getAffList()
        self.aplusAffList = aplusObj.affListWithSenetence

        # for teiAff, aplusAff in zip(self.teiAffList, self.aplusAffList):
            # if(self.doesTeiAndAplusTextMatch(teiAff, aplusAff)):
            # self.startComparing(teiAff, aplusAff)
            # print 'TRUE'
            # else:
            #     print 'FALSE'
        for teiAff in self.teiAffList:
            self.doesTeiWordInAplus(teiAff)

    def doesTeiWordInAplus(self,teiAff):
        for tagVal in teiAff.tagValueList:
            for word in tagVal.text.split(' '):
                for aplusAff in self.aplusAffList:
                    for aplusTagVal in aplusAff.tagValueList:
                        if word in aplusTagVal.text:
                            print tagVal.name, word, tagVal.text



    def doesTeiAndAplusTextMatch(self, teiText, aplusText):
        #  we will use alogo one we will finilse the approach
        if(teiText in aplusText or aplusText in teiText):
            return True
        return False

    def startComparing(self,teiAff, aplusAff):
        self.rule1(teiAff.tagValueList, aplusAff.tagValueList)
        if(self.doesAllTagMatchInAplus(teiAff.tagValueList)):
            pass

        print ''
        # index = 0
        # hashMap = []
        # for tagValue in aplusTagValueList:
        #     hashValue = '#{0}'.format(index)
        #     sentence.replace(tagValue.text,hashValue)
    def rule1(self, teiList, aplusList):
        for aplus in aplusList:
            for index, tei in enumerate(teiList):
                if tei.text in aplus.text:
                    teiList[index].matchTagName = aplus.name
                    teiList[index].isMatch = True

    def doesAllTagMatchInAplus(self,teiList):
        return [tei for tei in teiList if 'marker' not in tei.name and  tei.isMatch == False] > 0








if __name__ == '__main__':
    uitlty = utility('/Users/gpm1181/Documents/ws/grobid-output__oo/122_2017_2860.affiliation.tei.xml', '/Users/gpm1181/Documents/ws/a-plus/122_2017_2860.xml')
    uitlty.iterateAff()