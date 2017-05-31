<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:es="http://xbib.org/ns/sru/elasticsearch/source/1.0/"
                xmlns:mods="http://www.loc.gov/mods/v3"
                exclude-result-prefixes="es">

    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/*">
        <mods:mods version="3.6" xmlns:mods="http://www.loc.gov/mods/v3" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-6.xsd">
            <xsl:call-template name="titleInfo"/>
            <xsl:call-template name="name"/>
            <xsl:call-template name="typeOfResource"/>
            <xsl:call-template name="genre"/>
            <xsl:call-template name="originInfo"/>
            <xsl:call-template name="language"/>
            <xsl:call-template name="physicalDescription"/>
            <xsl:call-template name="abstract"/>
            <xsl:call-template name="tableOfContents"/>
            <xsl:call-template name="targetAudience"/>
            <xsl:call-template name="note"/>
            <xsl:call-template name="subject"/>
            <xsl:call-template name="classification"/>
            <xsl:call-template name="relatedItem"/>
            <xsl:call-template name="identifier"/>
            <xsl:call-template name="location"/>
            <xsl:call-template name="accessCondition"/>
            <xsl:call-template name="part"/>
            <xsl:call-template name="extension"/>
            <xsl:call-template name="recordInfo"/>
        </mods:mods>
    </xsl:template>

    <!-- Top Level Elements -->

    <!-- 1. titleInfo-->
    <xsl:template name="titleInfo">
        <xsl:apply-templates select="es:VolumeDesignation|es:TitleStatement[position()=2]|es:TitleStatement|es:TitleProper|es:TitleUniform|es:TitleParallel|es:TitleParallelStatement|es:SecondaryForm"/>
    </xsl:template>
    <xsl:template match="es:VolumeDesignation">
        <mods:titleInfo>
            <xsl:call-template name="partNumber"/>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:TitleStatement">
        <mods:titleInfo>
            <mods:title>
                <xsl:value-of select="es:title"/>
            </mods:title>
            <xsl:call-template name="partNumber"/>
            <xsl:call-template name="subTitle"/>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:SecondaryForm">
        <mods:titleInfo>
            <mods:title>
                <xsl:value-of select="es:secondaryForm"/>
            </mods:title>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:TitleUniform">
        <mods:titleInfo type="uniform">
            <mods:title>
                <xsl:value-of select="es:titleUniform"/>
            </mods:title>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:TitleParallel">
        <mods:titleInfo type="alternative">
            <mods:title>
                <xsl:value-of select="es:title"/>
            </mods:title>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:TitleParallelStatement">
        <mods:titleInfo type="alternative">
            <mods:title>
                <xsl:value-of select="es:title"/>
            </mods:title>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:TitleStatement[position()=2]">
        <mods:titleInfo>
            <mods:title>
                <xsl:value-of select="../es:TitleStatement[position()=2]/es:title"/>
            </mods:title>
            <xsl:call-template name="partNumber"/>
            <mods:partName>
                <xsl:value-of select="../es:TitleStatement[position()=1]/es:title"/>
            </mods:partName>
            <xsl:call-template name="subTitle"/>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template match="es:TitleProper">
        <mods:titleInfo type="heading">
            <mods:title>
                <xsl:value-of select="es:title"/>
            </mods:title>
            <xsl:call-template name="partNumber"/>
            <xsl:call-template name="subTitle"/>
        </mods:titleInfo>
    </xsl:template>
    <xsl:template name="partNumber">
        <xsl:if test="../es:VolumeDesignation">
            <xsl:for-each select="../es:VolumeDesignation/es:volumeDesignation">
                <mods:partNumber>
                    <xsl:value-of select="."/>
                </mods:partNumber>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>
    <xsl:template name="subTitle">
        <xsl:if test="../es:TitleAddendum and not(../es:TitleStatement[position()=2])">
            <mods:subTitle>
                <xsl:value-of select="../es:TitleAddendum/es:title"/>
            </mods:subTitle>
        </xsl:if>
    </xsl:template>

    <!-- 2. name -->

    <xsl:template name="name">
        <xsl:apply-templates select="es:Person|es:PersonCreator|es:PersonContributor|es:PersonInterpreter|es:PersonAddressee|es:CorporateBody"/>
    </xsl:template>
    <xsl:template match="es:Person|es:PersonCreator|es:PersonContributor|es:PersonInterpreter|es:PersonAddressee">
        <mods:name type="personal">
            <xsl:if test="es:identifierGND">
                <xsl:attribute name="authority">gnd</xsl:attribute>
                <xsl:attribute name="authorityURI">http://d-nb.info/gnd/</xsl:attribute>
                <xsl:attribute name="valueURI">http://d-nb.info/gnd/<xsl:value-of select="es:identifierGND"/></xsl:attribute>
            </xsl:if>
            <xsl:call-template name="personal">
                <xsl:with-param name="p" select="."/>
            </xsl:call-template>
        </mods:name>
    </xsl:template>
    <xsl:template name="personal">
        <xsl:param name="p"/>
        <mods:namePart>
            <xsl:value-of select="$p/es:name"/>
        </mods:namePart>
        <xsl:if test="$p/es:numbering">
            <mods:namePart type="termsOfAddress">
                <xsl:value-of select="$p/es:numbering"/>
            </mods:namePart>
        </xsl:if>
        <xsl:if test="$p/es:title">
            <mods:namePart type="termsOfAddress">
                <xsl:value-of select="$p/es:title"/>
            </mods:namePart>
        </xsl:if>
        <xsl:if test="$p/es:bio">
            <mods:namePart type="date">
                <xsl:value-of select="$p/es:bio"/>
            </mods:namePart>
        </xsl:if>
        <xsl:if test="$p/es:role">
            <mods:role>
                <mods:roleTerm type="text">
                    <xsl:value-of select="$p/es:role"/>
                </mods:roleTerm>
            </mods:role>
        </xsl:if>
        <xsl:if test="$p = 'es:PersonContributor'">
            <mods:role>
                <mods:roleTerm type="code" authority="marcrelator">ctb</mods:roleTerm>
            </mods:role>
        </xsl:if>
    </xsl:template>
    <xsl:template match="es:CorporateBody">
        <xsl:choose>
        <xsl:when test="es:conferenceName">
            <mods:name type="conference">
                <mods:namePart>
                    <xsl:for-each select="es:conferenceName|es:addendum|es:unit|es:date|es:geoName">
                        <xsl:value-of select="."/>
                        <xsl:if test="position() != last()">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </mods:namePart>
            </mods:name>
        </xsl:when>
        <xsl:otherwise>
            <mods:name type="corporate">
                <mods:namePart>
                    <xsl:for-each select="es:name|es:addendum|es:unit">
                        <xsl:value-of select="."/>
                        <xsl:if test="position() != last()">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </mods:namePart>
            </mods:name>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- 3. typeOfResource -->

    <xsl:template name="typeOfResource">
        <xsl:variable name="format" select="es:rda/es:content"/>
        <xsl:choose>
            <xsl:when test="$format = 'Text'">
                <mods:typeOfResource>text</mods:typeOfResource>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- 4. genre -->

    <xsl:template name="genre">
        <xsl:apply-templates select="es:dc/es:type"/>
    </xsl:template>
    <xsl:template match="es:dc/es:type">
        <mods:genre>
            <xsl:attribute name="authority">mab</xsl:attribute>
            <xsl:value-of select="."/>
        </mods:genre>
    </xsl:template>

    <!-- 5. originInfo-->

    <xsl:template name="originInfo">
        <mods:originInfo>
            <xsl:apply-templates select="es:PublicationPlace|es:PublisherName|es:Date|es:DateFirst|es:DateProper|es:Edition|es:TypePeriodical|es:TypeMonograph"/>
        </mods:originInfo>
    </xsl:template>
    <xsl:template match="es:PublicationPlace">
        <mods:place>
            <mods:placeTerm type="text">
                <xsl:value-of select="es:printingPlace"/>
            </mods:placeTerm>
        </mods:place>
    </xsl:template>
    <xsl:template match="es:PublisherName">
        <mods:publisher>
            <xsl:value-of select="es:name|es:printerName"/>
        </mods:publisher>
    </xsl:template>
    <xsl:template match="es:Date">
        <mods:dateIssued>
            <xsl:value-of select="es:date"/>
        </mods:dateIssued>
    </xsl:template>
    <xsl:template match="es:DateFirst">
        <xsl:choose>
            <xsl:when test="../es:DateLast">
                <mods:dateIssued point="start">
                    <xsl:value-of select="es:date"/>
                </mods:dateIssued>
                <mods:dateIssued point="end">
                    <xsl:value-of select="../es:DateLast/es:date"/>
                </mods:dateIssued>
            </xsl:when>
            <xsl:otherwise>
                <mods:dateIssued>
                    <xsl:value-of select="es:date"/>
                </mods:dateIssued>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="es:DateProper">
        <mods:dateIssued keyDate="yes">
            <xsl:value-of select="es:date"/>
        </mods:dateIssued>
    </xsl:template>
    <xsl:template match="es:Edition">
        <mods:edition>
            <xsl:value-of select="es:edition"/>
        </mods:edition>
    </xsl:template>
    <xsl:template match="es:TypePeriodical|es:TypeMonograph">
        <xsl:variable name="type" select="."/>
        <xsl:choose>
            <xsl:when test="contains($type, 'eitschrift')">
                <mods:issuance>serial</mods:issuance>
            </xsl:when>
            <xsl:when test="contains($type, 'mehrbändiges')">
                <mods:issuance>multipart monograph</mods:issuance>
            </xsl:when>
            <xsl:when test="contains($type, 'Schriftenreihe')">
                <mods:issuance>continuing</mods:issuance>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- 6. language -->

    <xsl:template name="language">
        <xsl:apply-templates select="es:Language"/>
    </xsl:template>
    <xsl:template match="es:Language">
        <mods:language>
            <mods:languageTerm>
                <xsl:attribute name="type">code</xsl:attribute>
                <xsl:attribute name="authority">iso639-2b</xsl:attribute>
                <xsl:value-of select="es:languageSource"/>
            </mods:languageTerm>
            <mods:languageTerm>
                <xsl:attribute name="type">text</xsl:attribute>
                <xsl:value-of select="es:language"/>
            </mods:languageTerm>
        </mods:language>
    </xsl:template>

    <!-- 7. physicalDescription -->

    <xsl:template name="physicalDescription">
        <xsl:if test="es:Extent|es:VolumeExtent|es:ArticleExtent|es:PieceExtent|es:dc/es:format">
            <mods:physicalDescription>
                <xsl:apply-templates select="es:Extent|es:VolumeExtent|es:ArticleExtent|es:PieceExtent|es:dc/es:format"/>
            </mods:physicalDescription>
        </xsl:if>
    </xsl:template>
    <xsl:template match="es:dc/es:format">
        <mods:form authority="mab">
            <xsl:value-of select="."/>
        </mods:form>
    </xsl:template>
    <xsl:template match="es:Extent|es:VolumeExtent|es:ArticleExtent|es:PieceExtent">
            <mods:extent>
                <xsl:value-of select="es:extent"/>
            </mods:extent>
    </xsl:template>

    <!-- 8. abstract -->

    <xsl:template name="abstract">
        <xsl:apply-templates select="es:Abstract|es:Excerpt|es:Recension"/>
    </xsl:template>
    <xsl:template match="es:Abstract|es:Excerpt|es:Recension">
        <mods:abstract>
            <xsl:value-of select="es:abstract"/>
        </mods:abstract>
    </xsl:template>

    <!-- 9. tableOfContents -->

    <xsl:template name="tableOfContents">
    </xsl:template>

    <!-- 10. targetAudience -->

    <xsl:template name="targetAudience">
    </xsl:template>

    <!-- 11. note -->

    <xsl:template name="note">
        <xsl:apply-templates select="es:CreatorStatement|es:TitleCreationStatement|es:Description|es:DescriptionMediaTypeElectronic|es:DescriptionOfAdditionalTitle|es:DescriptionOfExpression|es:DescriptionOfFormerEditionsOrVolumes|es:DescriptionOfIssuance|es:DescriptionOfPeriodicSupplements|es:DescriptionOfPublicationStatement|es:DescriptionOfRelatedEditions|es:DescriptionOfThesis|es:DescriptionOfTitleForms|es:DescriptionOfUniformTitle"/>
    </xsl:template>
    <xsl:template match="es:CreatorStatement|es:TitleCreationStatement">
        <mods:note type="statement of responsibility">
            <xsl:choose>
                <xsl:when test="local-name()='CreatorStatement'">
                    <xsl:value-of select="es:creatorStatement"/>
                </xsl:when>
                <xsl:when test="local-name()='TitleCreationStatement'">
                    <xsl:value-of select="es:title"/>
                </xsl:when>
            </xsl:choose>
        </mods:note>
    </xsl:template>
    <xsl:template match="es:Description|es:DescriptionMediaTypeElectronic|es:DescriptionOfAdditionalTitle|es:DescriptionOfExpression|es:DescriptionOfFormerEditionsOrVolumes|es:DescriptionOfIssuance|es:DescriptionOfPeriodicSupplements|es:DescriptionOfPublicationStatement|es:DescriptionOfRelatedEditions|es:DescriptionOfThesis|es:DescriptionOfTitleForms|es:DescriptionOfUniformTitle">
        <xsl:variable name="type" select="local-name()"/>
        <mods:note type="{$type}">
            <xsl:for-each select="es:prefix|es:description|es:note">
                <xsl:value-of select="."/>
                <xsl:if test="position() != last()">
                    <xsl:text> </xsl:text>
                </xsl:if>
            </xsl:for-each>
        </mods:note>
    </xsl:template>

    <!-- 12. subject -->

    <xsl:template name="subject">
        <xsl:apply-templates select="es:SubjectHeadingSequence"/>
        <xsl:apply-templates select="es:SubjectTopic|es:SubjectRSWK|es:SubjectOther|es:SubjectHeadings|es:SubjectGeo|es:SubjectTitle|es:SubjectPersonalName|es:SubjectCorporateBodyName"/>
    </xsl:template>
    <xsl:template match="es:SubjectHeadingSequence">
        <mods:subject authority="rswk">
            <xsl:apply-templates/>
        </mods:subject>
    </xsl:template>
    <xsl:template match="es:SubjectTopic|es:SubjectRSWK|es:SubjectOther|es:SubjectHeadings|es:SubjectGeo|es:SubjectTitle|es:SubjectPersonalName|es:SubjectCorporateBodyName">
        <mods:subject>
            <xsl:apply-templates/>
        </mods:subject>
    </xsl:template>
    <xsl:template match="es:subjectTopic|es:subjectTopicName|es:subjectMusic|es:subjectMusicArrangement|es:subjectMusicKey|es:subjectNumbering|es:subjectConference|es:subjectUnit|es:subjectHeading|es:subjectLanguage|es:subjectNameTitle|es:subjectTitle|es:subjectOther|es:subject">
        <mods:topic>
            <xsl:value-of select="."/>
            <xsl:for-each select="following-sibling::es:subjectAddendum|preceding-sibling::es:subjectAddendum">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </mods:topic>
    </xsl:template>
    <xsl:template match="es:subjectGeo|es:subjectGeoName">
        <mods:geographic>
            <xsl:value-of select="."/>
            <xsl:for-each select="following-sibling::es:subjectAddendum|following-sibling::es:subjectGeoAddendum|preceding-sibling::es:subjectAddendum|preceding-sibling::es:subjectGeoAddendum">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </mods:geographic>
    </xsl:template>
    <xsl:template match="es:subjectChronological|es:subjectDate">
        <mods:temporal>
            <xsl:value-of select="."/>
            <xsl:for-each select="following-sibling::es:subjectAddendum|preceding-sibling::es:subjectAddendum">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </mods:temporal>
    </xsl:template>
    <xsl:template match="es:subjectName">
        <mods:name>
            <xsl:if test="../es:identifierGND">
                <xsl:attribute name="authority">gnd</xsl:attribute>
                <xsl:attribute name="authorityURI">http://d-nb.info/gnd/</xsl:attribute>
                <xsl:attribute name="valueURI">
                    http://d-nb.info/gnd/<xsl:value-of select="../es:identifierGND"/>
                </xsl:attribute>
            </xsl:if>
            <mods:namePart>
                <xsl:value-of select="."/>
                <xsl:for-each select="following-sibling::es:subjectNameAddendum|following-sibling::es:subjectPersonalNameAddendum|preceding-sibling::es:subjectNameAddendum|preceding-sibling::es:subjectPersonalNameAddendum">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </mods:namePart>
        </mods:name>
    </xsl:template>
    <xsl:template match="es:subjectPersonName|es:subjectPersonalName">
        <mods:name type="personal">
            <xsl:if test="../es:identifierGND">
                <xsl:attribute name="authority">gnd</xsl:attribute>
                <xsl:attribute name="authorityURI">http://d-nb.info/gnd/</xsl:attribute>
                <xsl:attribute name="valueURI">
                    http://d-nb.info/gnd/<xsl:value-of select="../es:identifierGND"/>
                </xsl:attribute>
            </xsl:if>
            <mods:namePart>
                <xsl:value-of select="."/>
                <xsl:for-each select="following-sibling::es:subjectNameAddendum|following-sibling::es:subjectPersonalNameAddendum|preceding-sibling::es:subjectNameAddendum|preceding-sibling::es:subjectPersonalNameAddendum">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </mods:namePart>
        </mods:name>
    </xsl:template>
    <xsl:template match="es:subjectCorporateBodyName|es:subjectCorporateUnit">
        <mods:name type="corporate">
            <xsl:if test="../es:identifierGND">
                <xsl:attribute name="authority">gnd</xsl:attribute>
                <xsl:attribute name="authorityURI">http://d-nb.info/gnd/</xsl:attribute>
                <xsl:attribute name="valueURI">
                    http://d-nb.info/gnd/<xsl:value-of select="../es:identifierGND"/>
                </xsl:attribute>
            </xsl:if>
            <mods:namePart>
                <xsl:value-of select="."/>
                <xsl:for-each select="following-sibling::es:subjectNameAddendum|following-sibling::es:subjectPersonalNameAddendum|preceding-sibling::es:subjectNameAddendum|preceding-sibling::es:subjectPersonalNameAddendum">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="."/>
                </xsl:for-each>
            </mods:namePart>
        </mods:name>
    </xsl:template>
    <xsl:template match="es:subjectGenre">
        <mods:genre>
            <xsl:value-of select="."/>
            <xsl:for-each select="following-sibling::es:subjectAddendum|preceding-sibling::es:subjectAddendum">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </mods:genre>
    </xsl:template>

    <!-- 13. classification -->

    <xsl:template name="classification">
    </xsl:template>

    <!-- 14. relatedItem -->

    <xsl:template name="relatedItem">
        <xsl:apply-templates select="es:SeriesAddedEntryUniformTitle"/>
        <xsl:if test="es:TitleSuper|es:TitleSuperUniform">
            <mods:relatedItem type="series">
                <mods:titleInfo>
                    <mods:title>
                        <xsl:variable name="title">
                            <xsl:choose>
                                <xsl:when test="es:TitleSuper">
                                    <xsl:value-of select="es:TitleSuper/es:titleSuper"/>
                                </xsl:when>
                                <xsl:when test="es:TitleSuperUniform">
                                    <xsl:value-of select="es:TitleSuperUniform/es:uniformTitle"/>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when test="es:TitleSuperVolumeDesignation">
                                <xsl:variable name="tsvd" select="es:TitleSuperVolumeDesignation/es:volumeDesignation"/>
                                <xsl:choose>
                                    <xsl:when test="contains($title, $tsvd)">
                                        <xsl:value-of select="$title"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="concat($title, ' ; ', $tsvd)"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$title"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </mods:title>
                </mods:titleInfo>
                <xsl:if test="es:TitleSuperIdentifier">
                    <xsl:variable name="isil">
                        <xsl:value-of select="substring-after(substring-before(es:TitleSuperIdentifier/es:titleSuperIdentifier, ')'), '(')"/>
                    </xsl:variable>
                    <mods:identifier type="{$isil}">
                        <xsl:value-of select="substring-after(es:TitleSuperIdentifier/es:titleSuperIdentifier, ')')"/>
                    </mods:identifier>
                </xsl:if>
            </mods:relatedItem>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="es:VolumeDesignation">
                <mods:relatedItem type="host">
                    <xsl:choose>
                        <xsl:when test="es:TitleStatement[position() = 2]">
                            <mods:titleInfo>
                                <mods:title>
                                    <xsl:value-of select="es:TitleStatement[position() = 2]/es:titleMain"/>
                                </mods:title>
                            </mods:titleInfo>
                        </xsl:when>
                        <xsl:when test="es:TitleStatement[position() = 1]">
                            <mods:titleInfo>
                                <mods:title>
                                    <xsl:value-of select="es:TitleStatement[position() = 1]/es:titleMain"/>
                                </mods:title>
                            </mods:titleInfo>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:variable name="isil">
                        <xsl:value-of
                                select="substring-after(substring-before(es:RecordIdentifierSuper/es:recordIdentifierSuper, ')'), '(')"/>
                    </xsl:variable>
                    <mods:identifier type="{$isil}">
                        <xsl:value-of select="substring-after(es:RecordIdentifierSuper/es:recordIdentifierSuper, ')')"/>
                    </mods:identifier>
                </mods:relatedItem>
            </xsl:when>
            <xsl:when test="es:RecordIdentifierSuper">
                <mods:relatedItem type="host">
                    <xsl:variable name="isil">
                        <xsl:value-of
                                select="substring-after(substring-before(es:RecordIdentifierSuper/es:recordIdentifierSuper, ')'), '(')"/>
                    </xsl:variable>
                    <mods:identifier type="{$isil}">
                        <xsl:value-of select="substring-after(es:RecordIdentifierSuper/es:recordIdentifierSuper, ')')"/>
                    </mods:identifier>
                </mods:relatedItem>
            </xsl:when>
        </xsl:choose>
        <xsl:if test="es:SecondaryEdition">
            <mods:relatedItem type="otherFormat">
                <xsl:apply-templates select="es:SecondaryEdition"/>
                <xsl:if test="es:PublicationPlaceOfSecondaryEdition|es:PublisherNameOfSecondaryEdition|es:SecondaryEditionPublicationDate">
                    <mods:originInfo>
                        <xsl:apply-templates select="es:PublicationPlaceOfSecondaryEdition|es:PublisherNameOfSecondaryEdition|es:SecondaryEditionPublicationDate"/>
                    </mods:originInfo>
                </xsl:if>
                <xsl:if test="es:SecondaryEditionExtent">
                    <mods:physicalDescription>
                        <xsl:apply-templates select="es:SecondaryEditionExtent"/>
                    </mods:physicalDescription>
                </xsl:if>
                <xsl:apply-templates select="es:IdentifierISBNSecondaryForm"/>
            </mods:relatedItem>
        </xsl:if>
    </xsl:template>
    <xsl:template match="es:PublicationPlaceOfSecondaryEdition">
        <mods:place>
            <mods:placeTerm type="text">
                <xsl:value-of select="es:name"/>
            </mods:placeTerm>
        </mods:place>
    </xsl:template>
    <xsl:template match="es:PublisherNameOfSecondaryEdition">
        <mods:publisher>
            <xsl:value-of select="es:name"/>
        </mods:publisher>
    </xsl:template>
    <xsl:template match="es:SecondaryEditionPublicationDate">
        <mods:dateIssued>
            <xsl:value-of select="es:secondaryEdition"/>
        </mods:dateIssued>
    </xsl:template>
    <xsl:template match="es:SecondaryEditionExtent">
        <mods:extent>
            <xsl:value-of select="es:secondaryEdition"/>
        </mods:extent>
    </xsl:template>
    <xsl:template match="es:IdentifierISBNSecondaryForm">
        <mods:identifier type="isbn">
            <xsl:value-of select="es:identifierISBN"/>
        </mods:identifier>
    </xsl:template>
    <xsl:template match="es:SeriesAddedEntryUniformTitle">
        <mods:relatedItem type="series">
            <xsl:if test="es:title">
                <mods:titleInfo>
                    <mods:title>
                        <xsl:value-of select="es:title"/>
                    </mods:title>
                </mods:titleInfo>
            </xsl:if>
            <xsl:if test="es:designation">
                <xsl:variable name="isil">
                    <xsl:value-of select="substring-after(substring-before(es:designation, ')'), '(')"/>
                </xsl:variable>
                <xsl:if test="$isil = 'DE-605'">
                    <mods:identifier type="hbzId">
                        <xsl:value-of select="substring-after(es:designation, ')')"/>
                    </mods:identifier>
                </xsl:if>
            </xsl:if>
        </mods:relatedItem>
    </xsl:template>

    <!-- 15. identifier -->

    <xsl:template name="identifier">
        <xsl:apply-templates select="es:IdentifierISBN|es:IdentifierISSN|es:IdentifierSerial|es:IdentifierZDB|es:IdentifierDOI"/>
    </xsl:template>
    <xsl:template match="es:IdentifierISBN">
        <mods:identifier type="isbn">
            <xsl:value-of select="es:identifierISBN"/>
        </mods:identifier>
    </xsl:template>
    <xsl:template match="es:IdentifierISSN|es:IdentifierSerial">
        <mods:identifier type="issn">
            <xsl:value-of select="es:identifierISSN"/>
        </mods:identifier>
    </xsl:template>
    <xsl:template match="es:IdentifierZDB">
        <mods:identifier type="zdb">
            <xsl:value-of select="es:identifierZDB"/>
        </mods:identifier>
    </xsl:template>
    <xsl:template match="es:IdentifierDOI">
        <mods:identifier type="doi">
            <xsl:value-of select="es:standardNumber"/>
        </mods:identifier>
    </xsl:template>

    <!-- 16. location -->

    <xsl:template name="location">
        <xsl:if test="not(../es:holdings)">
            <xsl:apply-templates select="es:IdentifierLocal"/>
        </xsl:if>
        <xsl:apply-templates select="es:OnlineAccess"/>
        <xsl:apply-templates select="es:RecordIdentifier"/>
        <xsl:apply-templates select="descendant::es:Item"/>
        <xsl:apply-templates select="descendant::es:Shelfmark"/>
        <xsl:apply-templates select="descendant::es:OnlineAccessRemote"/>
    </xsl:template>
    <xsl:template match="es:IdentifierLocal">
        <xsl:variable name="isil">
            <xsl:value-of select="substring-after(substring-before(../es:RecordIdentifier/es:identifierForTheRecord, ')'), '(')"/>
        </xsl:variable>
        <xsl:if test="$isil != 'DE-605'">
            <mods:location>
                <mods:physicalLocation>
                    <xsl:value-of select="$isil"/>
                </mods:physicalLocation>
                <mods:holdingSimple>
                    <mods:copyInformation>
                        <mods:itemIdentifier type="identifierlocal">
                            <xsl:value-of select="."/>
                        </mods:itemIdentifier>
                    </mods:copyInformation>
                </mods:holdingSimple>
            </mods:location>
        </xsl:if>
    </xsl:template>
    <xsl:template match="es:OnlineAccess">
        <xsl:call-template name="locationUrl"/>
    </xsl:template>
    <xsl:template name="locationUrl">
        <xsl:variable name="dl">
            <xsl:choose>
                <xsl:when test="es:nonpublicnote">
                    <xsl:value-of select="es:nonpublicnote"/>
                </xsl:when>
                <xsl:when test="es:publicnote">
                    <xsl:value-of select="es:publicnote"/>
                </xsl:when>
                <xsl:when test="es:access and not(starts-with(es:access, 'http'))">
                    <xsl:value-of select="es:access"/>
                </xsl:when>
                <xsl:when test="es:relatedto and not(starts-with(es:relatedto, 'http'))">
                    <xsl:value-of select="es:access"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="uri">
            <xsl:choose>
                <xsl:when test="es:uri">
                    <xsl:value-of select="es:uri"/>
                </xsl:when>
                <xsl:when test="es:access">
                    <xsl:value-of select="es:access"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="$uri != ''">
            <mods:location>
                <mods:url>
                    <xsl:if test="$dl !=''">
                        <xsl:attribute name="displayLabel">
                            <xsl:value-of select="$dl"/>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="$uri"/>
                </mods:url>
            </mods:location>
        </xsl:if>
    </xsl:template>
    <xsl:template match="es:RecordIdentifier">
        <mods:recordInfo>
            <mods:recordIdentifier>
                <xsl:attribute name="source">
                    <xsl:value-of select="substring-after(substring-before(es:identifierForTheRecord, ')'), '(')"/>
                </xsl:attribute>
                <xsl:value-of select="substring-after(es:identifierForTheRecord, ')')"/>
            </mods:recordIdentifier>
        </mods:recordInfo>
    </xsl:template>
    <xsl:template match="es:Item">
        <mods:location>
            <mods:physicalLocation>
                <xsl:value-of select="es:identifier"/>
            </mods:physicalLocation>
            <mods:holdingSimple>
                <mods:copyInformation>
                    <xsl:if test="es:shelfmark">
                        <mods:subLocation>
                            <xsl:value-of select="es:shelfmark"/>
                        </mods:subLocation>
                    </xsl:if>
                    <xsl:if test="es:callnumber">
                        <mods:shelfLocator>
                            <xsl:value-of select="es:callnumber"/>
                        </mods:shelfLocator>
                    </xsl:if>
                    <xsl:for-each select="es:interlibraryservice">
                        <mods:note type="interlibraryservice">
                            <xsl:value-of select="."/>
                        </mods:note>
                    </xsl:for-each>
                    <xsl:if test="es:status">
                        <mods:note type="status">
                            <xsl:value-of select="es:status"/>
                        </mods:note>
                    </xsl:if>
                    <mods:itemIdentifier type="isil">
                        <xsl:value-of select="es:identifier"/>
                    </mods:itemIdentifier>
                </mods:copyInformation>
            </mods:holdingSimple>
        </mods:location>
    </xsl:template>
    <xsl:template match="es:Shelfmark">
        <xsl:variable name="isil">
            <xsl:value-of select="substring-after(substring-before(../../es:id, ')'), '(')"/>
        </xsl:variable>
        <mods:location>
            <mods:physicalLocation>
                <xsl:value-of select="$isil"/>
            </mods:physicalLocation>
            <mods:holdingSimple>
                <mods:copyInformation>
                    <mods:itemIdentifier type="isil">
                        <xsl:value-of select="$isil"/>
                    </mods:itemIdentifier>
                    <mods:shelfLocator>
                        <xsl:value-of select="."/>
                    </mods:shelfLocator>
                </mods:copyInformation>
            </mods:holdingSimple>
        </mods:location>
    </xsl:template>
    <xsl:template match="es:OnlineAccessRemote">
        <xsl:if test="not(es:identifier)">
            <xsl:call-template name="locationUrl"/>
        </xsl:if>
    </xsl:template>

    <!-- 17. accessCondition -->
    <xsl:template name="accessCondition">
    </xsl:template>

    <!-- 18. part -->
    <xsl:template name="part">
    </xsl:template>

    <!-- 19. extension -->
    <xsl:template name="extension">
    </xsl:template>

    <!-- 20. recordInfo -->
    <xsl:template name="recordInfo">
    </xsl:template>

    <!-- swallow loners (shouldn't happen) -->
    <xsl:template match="text()"/>

</xsl:stylesheet>
