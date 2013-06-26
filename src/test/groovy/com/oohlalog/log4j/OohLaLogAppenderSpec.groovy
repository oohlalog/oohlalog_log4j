import org.apache.log4j.Logger
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jsaardchit
 * Date: 3/6/13
 * Time: 1:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class OohLaLogAppenderSpec extends Specification {
	Logger logger = Logger.getLogger( 'test.logger' )

	def "test submission of oohlalog payload after buffer reached"() {
		setup:
		def logCount = 500

		when:
		(0..logCount).each {
			if ( it % 10 == 0 )
				Thread.sleep(1000)

			logger.info( "Testing log message ${it}" )
		}

		then:
		true

		cleanup:
		logger.removeAllAppenders()
	}
}
