package org.thingsboard.ai.mcp.server.tools.tesenso.license;

import com.tesenso.server.license.guard.LicenseGuard;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * Startsperre ohne gueltige Tesenso-Lizenz (license-core), wie in den anderen Microservices.
 *
 * <p>Nicht in {@code McpServerApplication.main}, sondern hier: dieser Fork aendert keine
 * Upstream-Datei (TESENSO.md), registriert wird der Listener ueber
 * {@code META-INF/spring.factories}.
 *
 * <p>Der Zeitpunkt ist gewaehlt: nach der Einrichtung der Protokollierung
 * (LoggingApplicationListener laeuft beim selben Ereignis mit hoeherer Prioritaet). Vorher
 * schriebe Logback auf die Standardausgabe, und im STDIO-Modus ist das der Kanal des
 * MCP-Protokolls -- eine einzige Protokollzeile dort zerstoert die Verbindung. Danach geht die
 * Konsole nach stderr (logback-spring.xml).
 */
public class TesensoLicenseGuardListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static volatile boolean done;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // Nur einmal je Prozess, auch wenn ein Kind-Kontext dasselbe Ereignis noch einmal ausloest.
        if (done) {
            return;
        }
        LicenseGuard.awaitValidLicense("tesenso-mcp");
        done = true;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
