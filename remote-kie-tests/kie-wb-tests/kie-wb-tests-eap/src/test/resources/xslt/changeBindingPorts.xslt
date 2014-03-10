<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add the security domains to the standalone.xml used during 
	the integration tests. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:as="urn:jboss:domain:1.2" xmlns:sd="urn:jboss:domain:security:1.1"
	version="1.0">

	<xsl:output method="xml" indent="yes" />

	<xsl:template match="//as:socket-binding-group[@name='standard-sockets']" />

	<xsl:template match="//as:socket-binding-group">
		<socket-binding-group name="standard-sockets"
			default-interface="public" port-offset="0">
			<socket-binding name="management-native" interface="management"
				port="29999" />
			<socket-binding name="management-http" interface="management"
				port="29990" />
			<socket-binding name="management-https" interface="management"
				port="29443" />
			<socket-binding name="ajp" port="28009" />
			<socket-binding name="http" port="28080" />
			<socket-binding name="https" port="28443" />
			<socket-binding name="osgi-http" interface="management"
				port="8090" />
			<socket-binding name="remoting" port="24447" />
			<socket-binding name="txn-recovery-environment" port="24712" />
			<socket-binding name="txn-status-manager" port="24713" />
			<outbound-socket-binding name="mail-smtp">
				<remote-destination host="localhost" port="25" />
			</outbound-socket-binding>
		</socket-binding-group>
	</xsl:template>

	<!-- Copy everything else. -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>