<?xml version="1.0" encoding="UTF-8"?>
<!--

gc_ddi_cv2html.xslt
2011-04-11

XSLT stylesheet for generating HTML files on the basis of files 
according to Genericode 1.0 DDICV profile 1.0.

Copyright (c) Joachim Wackerow
joachim.wackerow@gesis.org

Update 7.5.2012 (c) Jani Hautamäki / FSD
jani.hautamaki@uta.fi

Wrote a small modification which adds one more <h:div> under the genericode
Annotation/Description element. This new div has @class='VersionNotes' and
the contents are taken from the Identity spreadsheet's new cell called
"Version Notes in American English".

-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:gc="http://docs.oasis-open.org/codelist/ns/genericode/1.0/" xmlns:ddi-cv="urn:ddi-cv"
	xmlns="http://www.w3.org/1999/xhtml" xmlns:h="http://www.w3.org/1999/xhtml"
	xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="ddi-cv gc h xalan">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4"
		doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"/>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:variable name="DefaultLanguage" select=" 'en' "/>
	<xsl:param name="Language" select=" $DefaultLanguage "/>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:variable name="Value" select="/gc:CodeList/Annotation/AppInfo/ddi-cv:Value"/>
	<xsl:variable name="CopyrightText" select="$Value[ @key='CopyrightText' ]"/>
	<xsl:variable name="CopyrightOwner" select="$Value[ @key='CopyrightOwner' ]"/>
	<xsl:variable name="CopyrightOwnerUrl" select="$Value[ @key='CopyrightOwnerUrl' ]"/>
	<xsl:variable name="CopyrightYear" select="$Value[ @key='CopyrightYear' ]"/>
	<xsl:variable name="LicenseName" select="$Value[ @key='LicenseName' ]"/>
	<xsl:variable name="LicenseUrl" select="$Value[ @key='LicenseUrl' ]"/>
	<xsl:variable name="LicenseLogoUrl" select="$Value[ @key='LicenseLogoUrl' ]"/>
	<xsl:variable name="NonBreakingSpace" select=" '&#160;' "/>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:key name="ColumnRef" match="/gc:CodeList/SimpleCodeList/Row/Value" use="@ColumnRef"/>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="*">
		<xsl:element name="{local-name()}">
			<xsl:apply-templates select="@* | node()"/>
		</xsl:element>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="@*">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
		</xsl:copy>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="/">
		<xsl:apply-templates select="gc:CodeList"/>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="gc:CodeList">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="{$Language}">
			<head>
				<meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
				<title>
					<xsl:call-template name="Title"/>
				</title>
				<xsl:call-template name="Style"/>
			</head>
			<body>
				<xsl:call-template name="PageHeader"/>
				<hr/>
				<div class="CodeList">
					<table>
						<caption>Code List</caption>
						<thead>
							<tr>
								<xsl:apply-templates select="ColumnSet/Column" mode="Description"/>
							</tr>
						</thead>
						<tbody>
							<xsl:apply-templates select="SimpleCodeList/Row">
								<xsl:with-param name="ColumnSetNode" select="ColumnSet"/>
							</xsl:apply-templates>
						</tbody>
					</table>
				</div>
				<xsl:if test="Annotation/Description/h:div[ @class='Usage' ]">
					<xsl:call-template name="Usage"/>
				</xsl:if>
				<xsl:call-template name="PageFooter"/>
			</body>
		</html>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template name="Style">
		<style type="text/css">
			<xsl:text disable-output-escaping="yes"><![CDATA[

body {
  margin: 2em;
  /*
  font-size: 90%;
  */
}
h2, caption {
  font-size: 130%;
}
div.UsageTitle {
  font-size: 110%;
  font-weight: bold;
}
hr {
  clear: left;
  color: #D3D3D3; /*lightgrey */
  margin-top: 2em;
  margin-bottom: 2em;
  margin-left: 10%;
  margin-right: 10%;
}
table, th, td {
  border-style: solid;
  border-color: #D3D3D3; /*lightgrey */
  border-width: 1px;
  border-collapse: collapse;
}
caption {
  font-weight: bold;
  margin-bottom: 1em;
  text-align: left;
}
thead {
  font-size: 120%;
}
th, td {
  padding: 0.5em;
  vertical-align: top;
}
td.Code {
  font-weight: bold;
}
td.Term {
  white-space: nowrap;
}
dl {
  float: left;
}
dt {
  clear: left;
  float: left;
  font-weight: bold;
  width: 15em;
  margin-bottom: 0.3em;
  margin-right: 1em;
}
dd {
  float: left;
  margin: 0;
  margin-bottom: 0.3em;
}
span.LanguageCode{
  color: #D3D3D3; /*lightgrey */
  font-style: italic;
}
table.UsageDetails>thead>tr>th {
  width: 15em;
}
div.CodeList {
  page-break-before:always;
}
.URL {
  font-size:80%;
}

]]></xsl:text>
		</style>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template name="Title">
		<xsl:text>Controlled Vocabulary: </xsl:text>
		<xsl:call-template name="LanguageSelectorText">
			<xsl:with-param name="Node" select="Identification/LongName"/>
		</xsl:call-template>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template name="PageHeader">
		<div>
			<h1>
				<xsl:call-template name="Title"/>
			</h1>
			<hr/>
			<h2>Description</h2>
			<p>
				<xsl:call-template name="LanguageSelectorText">
					<xsl:with-param name="Node"
						select="Annotation/Description/h:div[ @class='Description' ]"/>
				</xsl:call-template>
				<xsl:apply-templates select="Annotation/Description/h:div[ @class='Description' ]"
					mode="Language">
					<xsl:sort select="@xml:lang"/>
				</xsl:apply-templates>
			</p>
			<xsl:if test="Annotation/Description/h:div[ @class='Note' ]">
				<h3>Note</h3>
				<xsl:call-template name="LanguageSelectorText">
					<xsl:with-param name="Node"
						select="Annotation/Description/h:div[ @class='Note' ]"/>
				</xsl:call-template>
				<xsl:apply-templates select="Annotation/Description/h:div[ @class='Note' ]"
					mode="Language">
					<xsl:sort select="@xml:lang"/>
				</xsl:apply-templates>
			</xsl:if>
			<xsl:if test="Annotation/Description/h:div[ @class='VersionNotes' ]">
				<h3>Version Notes</h3>
				<xsl:call-template name="LanguageSelectorText">
					<xsl:with-param name="Node"
						select="Annotation/Description/h:div[ @class='VersionNotes' ]"/>
				</xsl:call-template>
				<xsl:apply-templates select="Annotation/Description/h:div[ @class='VersionNotes' ]"
					mode="Language">
					<xsl:sort select="@xml:lang"/>
				</xsl:apply-templates>
			</xsl:if>
			<hr/>
			<h2>Details</h2>
			<dl>
				<dt>
					<xsl:text>Short Name:</xsl:text>
				</dt>
				<dd>
					<xsl:value-of select="Identification/ShortName"/>
				</dd>
				<dt>
					<xsl:text>Long Name:</xsl:text>
				</dt>
				<dd>
					<xsl:call-template name="LanguageSelectorText">
						<xsl:with-param name="Node" select="Identification/LongName"/>
					</xsl:call-template>
					<xsl:apply-templates select="Identification/LongName" mode="Language">
						<xsl:sort select="@xml:lang"/>
					</xsl:apply-templates>
				</dd>
				<dt>
					<xsl:text>Version:</xsl:text>
				</dt>
				<dd>
					<xsl:value-of select="Identification/Version"/>
				</dd>
				<dt>
					<xsl:text>Canonical URI:</xsl:text>
				</dt>
				<dd>
					<xsl:value-of select="Identification/CanonicalUri"/>
				</dd>
				<dt>
					<xsl:text>Canonical URI of this version:</xsl:text>
				</dt>
				<dd>
					<xsl:value-of select="Identification/CanonicalVersionUri"/>
				</dd>
				<xsl:apply-templates select="Identification/LocationUri"/>
				<xsl:apply-templates select="Identification/AlternateFormatLocationUri"/>
				<dt>
					<xsl:text>Agency Name: </xsl:text>
				</dt>
				<dd>
					<a href="{$CopyrightOwnerUrl}">
						<xsl:value-of select="Identification/Agency/ShortName"/>
					</a>
				</dd>
			</dl>
		</div>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="LocationUri">
		<dt>
			<xsl:text>Location URI:</xsl:text>
		</dt>
		<dd>
			<a class="URL">
				<xsl:attribute name="href">
					<xsl:value-of select="."/>
				</xsl:attribute>
				<xsl:value-of select="."/>
			</a>
		</dd>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="AlternateFormatLocationUri">
		<dt>
			<xsl:text>Alternate format location URI:</xsl:text>
		</dt>
		<dd>
			<a class="URL">
				<xsl:attribute name="href">
					<xsl:value-of select="."/>
				</xsl:attribute>
				<xsl:value-of select="."/>
			</a>
		</dd>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="Column" mode="Description">
		<th>
			<xsl:call-template name="LanguageSelectorText">
				<xsl:with-param name="Node" select="LongName"/>
			</xsl:call-template>
			<xsl:apply-templates select="LongName" mode="Language">
				<xsl:sort select="@xml:lang"/>
			</xsl:apply-templates>
		</th>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="Column" mode="Content">
		<xsl:param name="RowNode"/>
		<xsl:variable name="ColumnId" select="@Id"/>
		<xsl:variable name="Class" select="ShortName"/>
		<td class="{$Class}">
			<xsl:choose>
				<!-- if cell exists -->
				<xsl:when test="$RowNode/Value[ @ColumnRef = $ColumnId ]">
					<xsl:for-each select="$RowNode/Value[ @ColumnRef = $ColumnId ]">
						<xsl:choose>
							<xsl:when test="SimpleValue">
								<xsl:value-of select="SimpleValue"/>
							</xsl:when>
							<xsl:when test="ComplexValue">
								<xsl:call-template name="LanguageSelectorText">
									<xsl:with-param name="Node" select="ComplexValue/ddi-cv:Value"/>
								</xsl:call-template>
								<xsl:apply-templates select="ComplexValue/ddi-cv:Value"
									mode="Language">
									<xsl:sort select="@xml:lang"/>
								</xsl:apply-templates>
							</xsl:when>
						</xsl:choose>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>-</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</td>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="Row">
		<xsl:param name="ColumnSetNode"/>
		<xsl:variable name="RowNode" select="."/>
		<tr>
			<xsl:for-each select="$ColumnSetNode">
				<xsl:apply-templates select="Column" mode="Content">
					<xsl:with-param name="RowNode" select="$RowNode"/>
				</xsl:apply-templates>
			</xsl:for-each>
		</tr>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template name="Usage">
		<hr/>
		<div class="Usage">
			<h2>Usage</h2>
			<xsl:apply-templates select="Annotation/Description/h:div[ @class='Usage' ]/*"/>
		</div>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template name="PageFooter">
		<hr/>
		<h2>Copyright and License</h2>
		<p>
			<xsl:value-of select="$CopyrightText"/>
			<xsl:text> </xsl:text>
			<a href="{$CopyrightOwnerUrl}">
				<xsl:value-of select="$CopyrightOwner"/>
			</a>
			<xsl:text> </xsl:text>
			<xsl:value-of select="$CopyrightYear"/>
			<xsl:text>.</xsl:text>
		</p>
		<p>
			<a rel="license" href="{$LicenseUrl}">
				<img alt="{$LicenseName}" style="border-width:0" src="{$LicenseLogoUrl}"/>
			</a>
			<xsl:value-of select="$NonBreakingSpace"/>
			<xsl:value-of select="$NonBreakingSpace"/>
			<span>This work is licensed under a <a rel="license" href="{$LicenseUrl}">
					<xsl:value-of select="$LicenseName"/>
				</a>.</span>
		</p>
		<hr/>
		<p class="Generated">Page generated by gc_ddi-cv2html.xslt.</p>
		<!--
        <p class="Generated">Page generated by <a href="http://www.ddialliance.org/">gc_ddi-cv2html.xslt</a>.</p>
        -->
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template name="LanguageSelectorText">
		<xsl:param name="Node"/>
		<xsl:choose>
			<xsl:when test="$Node[ lang( $Language ) ]">
				<xsl:value-of select="$Node[ lang( $Language ) ]"/>
			</xsl:when>
			<xsl:when test="$Node[ lang( $Language ) ]">
				<xsl:value-of select="$Node[ lang( $DefaultLanguage ) ]"/>
			</xsl:when>
			<xsl:when test="$Node[ lang( 'en' ) ]">
				<xsl:value-of select="$Node[ lang( 'en' ) ]"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$Node[ not( @xml:lang ) ]"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<xsl:template match="node()" mode="Language">
		<xsl:param name="Separator" select=" 'Newline' "/>
		<xsl:if test=" @xml:lang != $DefaultLanguage ">
			<xsl:choose>
				<xsl:when test=" $Separator = 'Newline' ">
					<br/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$Separator"/>
				</xsl:otherwise>
			</xsl:choose>
			<span class="LanguageCode">
				<xsl:value-of select="@xml:lang"/>
				<xsl:text>: </xsl:text>
			</span>
			<xsl:value-of select="."/>
		</xsl:if>
	</xsl:template>
	<!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
</xsl:stylesheet>
