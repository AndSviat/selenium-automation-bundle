package com.sysgears.seleniumbundle.core.testng.commands

import com.sysgears.seleniumbundle.core.command.AbstractCommand
import com.sysgears.seleniumbundle.core.conf.Config
import com.sysgears.seleniumbundle.core.implicitinit.annotations.ImplicitInit
import com.sysgears.seleniumbundle.core.webdriver.MobileOptions
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

/**
 * Command class that provides method for testng.xml generation depending on given parameters.
 */
@Slf4j
class GenerateTestNGCommand extends AbstractCommand {

    /**
     * List of environments on which the tests should run. Environment consists of platform and browser.
     */
    @ImplicitInit(pattern = "(windows|mac|linux):(chrome|firefox|safari|MicrosoftEdge)")
    private List<String> environments

    /**
     * List of mobile devices which are specified in {@link MobileOptions} to run the tests in mobile emulation mode.
     */
    @ImplicitInit(pattern = "iphone5|iphone6|iphone6plus|ipad|ipadpro|s8plus")
    private List<String> devices

    /**
     * Creates an instance of GenerateTestNGCommand.
     *
     * @param arguments map with arguments of the command
     * @param conf project properties
     *
     * @throws IllegalArgumentException is thrown in case a value is missing for a mandatory parameter or
     * the value doesn't match the validation pattern
     */
    GenerateTestNGCommand(Map<String, List<String>> arguments, Config conf) throws IllegalArgumentException {
        super(arguments, conf)
    }

    /**
     * Executes testng.xml generation for parallel launch.
     *
     * @throws IOException in case if any error occurs while file write operation
     */
    @Override
    void execute() throws IOException {

        log.info("TestNG config generation has been started...")

        def params = environments.collect {
            def pair = it.split(":")
            [platform: pair.first(), browser: pair.last(), device: null]
        }

        params += devices.collect {
            [platform: "linux", browser: "chrome", device: it.toLowerCase()]
        }

        def threadCount = params.size()

        def res = new StringWriter().with { sw ->
            new MarkupBuilder(sw).suite(name: "Suite", parallel: "tests", "thread-count": "$threadCount") {

                params.each { Map<String, String> param ->
                    def testName = "${param.platform}-${param.device ?: param.browser}"

                    test(name: testName) {
                        param.entrySet().each { Map.Entry entry ->
                            if (entry.value) {
                                parameter(name: entry.key, value: entry.value)
                            }
                        }
                        packages {
                            "package"(name: "com.sysgears.seleniumbundle.tests.*")
                        }
                    }
                }
            }
            "<!DOCTYPE suite SYSTEM 'http://testng.org/testng-1.0.dtd'>\n\n" + sw.toString()
        }

        new File("src/test/resources/testngParallelLaunch.xml").write(res)

        log.info("TestNG config generation has been completed.")
    }
}