import org.apache.log4j.Logger
import org.apache.log4j.NDC
import spock.lang.Specification
import com.oohlalog.log4j.CountLevel

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
		def logCount = 100

		when:
		(0..logCount).each {
			if ( it % 10 == 0 ) {
				Thread.sleep(1000)
			}

			logger.info( "Testing log message ${it}" )
		}

		then:
		true

		cleanup:
		true
	}

	def "test submission of oohlalog payload with NDC token"() {
		setup:
		def logCount = 500
		org.apache.log4j.NDC.push('0s')			

		when:
		(0..logCount).each {
			if ( it % 10 == 0 ) {
				org.apache.log4j.NDC.pop()
				org.apache.log4j.NDC.push(it.toString()+'s')			
			}
			logger.info( "Testing NDC token #${it}" )
		}
		Thread.sleep(5500)

		then:
		true

		cleanup:
		true
	}

	def "test submission of oohlalog payload with MDC token"() {
		setup:
		def logCount = 500
		org.apache.log4j.MDC.put('token', '0s')			

		when:
		(0..logCount).each {
			if ( it % 10 == 0 ) {
				org.apache.log4j.MDC.put('token', it.toString()+'s')			
			}
			logger.info( "Testing MDC token #${it}" )
		}
		Thread.sleep(5500)

		then:
		true

		cleanup:
		true
	}


	def "test count"() {
		setup:
		def logCount = 100
		true
		when:
		(0..logCount).each {

			logger.log(CountLevel.COUNT,"Test 4J Counter")
		}
		Thread.sleep(5500)

		then:
		true

		cleanup:
		true
	}
}
