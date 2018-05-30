from entity.tagValue import tagValue

class matchTagValue(tagValue):
    def __init__(self,cName='',cText='',cmatchTagName='', cIsMatch=False):
        tagValue.__init__(self,cName,cText)
        self.matchTagName = cmatchTagName
        self.isMatch = cIsMatch
