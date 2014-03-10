<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add the a the PicketLink Extension to the standalone.xml 
	of the JBoss AS7 installation. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:as="urn:jboss:domain:1.2" version="1.0">

	<xsl:output method="xml" indent="yes" />

	<!-- If the extension is already defined, remove it to configure it again. -->
	<xsl:template match="//as:server/as:system-properties" />

	<xsl:template match="//as:server/as:extensions">
		<extensions>
			<xsl:apply-templates select="@* | *" />
		</extensions>
		<system-properties>
			<property name="idp.url" value="http://192.168.1.100:28080/idp/" />
			<property name="idp-sig.url" value="http://192.168.1.100:28080/idp-sig/" />
			<property name="sales-post.url" value="http://192.168.1.100:28080/sales-post/" />
			<property name="sales-saml11.url" value="http://192.168.1.100:28080/sales-saml11" />
			<property name="sales-post-valve.url" value="http://192.168.1.100:28080/sales-post-valve/" />
			<property name="sales-post-sig.url" value="http://192.168.1.100:28080/sales-post-sig/" />
			<property name="employee.url" value="http://192.168.1.100:28080/employee/" />
			<property name="employee-redirect-valve.url" value="http://192.168.1.100:28080/employee-redirect-valve/" />
			<property name="employee-sig.url" value="http://192.168.1.100:28080/employee-sig/" />
		</system-properties>
	</xsl:template>

	<!-- Copy everything else. -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>