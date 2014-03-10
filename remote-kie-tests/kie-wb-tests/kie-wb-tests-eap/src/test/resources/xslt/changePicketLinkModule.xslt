<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add the security domains to the standalone.xml used during 
	the integration tests. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:module="urn:jboss:module:1.1" version="1.0">
	
	<xsl:param name="picketlink_version" />
	
	<xsl:output method="xml" indent="yes" />

	<xsl:template match="//module:module[@name='org.picketlink']" />

	<xsl:template match="/">
		<module xmlns="urn:jboss:module:1.1" name="org.picketlink">
			<resources>
			    <resource-root>
			    	<xsl:attribute name="path">picketlink-jbas7-<xsl:value-of select="$picketlink_version"/>.jar</xsl:attribute>
			    </resource-root>
			    <resource-root>
			    	<xsl:attribute name="path">picketlink-core-<xsl:value-of select="$picketlink_version"/>.jar</xsl:attribute>
			    </resource-root>
			</resources>

			<dependencies>
		        <module name="javax.api"/>
		        <module name="javax.security.auth.message.api"/>
		        <module name="javax.security.jacc.api"/>
		        <module name="javax.transaction.api"/>
		        <module name="javax.xml.bind.api"/>
		        <module name="javax.xml.stream.api"/>
		        <module name="javax.servlet.api"/>
		        <module name="org.jboss.common-core"/>
		        <module name="org.jboss.logging"/>
		        <module name="org.jboss.as.web"/>
		        <module name="org.jboss.security.xacml"/>
		        <module name="org.picketbox"/>
		        <module name="javax.xml.ws.api"/>
		        <module name="org.apache.log4j"/>
		        <module name="org.apache.santuario.xmlsec"/>
				<module name="org.jboss.ws.api" />
				<module name="org.jboss.ws.spi" />
				<module name="org.apache.cxf" />
			</dependencies>
		</module>
	</xsl:template>

	<!-- Copy everything else. -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>