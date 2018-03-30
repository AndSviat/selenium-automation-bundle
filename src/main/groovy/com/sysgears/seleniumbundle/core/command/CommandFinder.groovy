package com.sysgears.seleniumbundle.core.command

import com.sysgears.seleniumbundle.core.conf.Config
import com.sysgears.seleniumbundle.core.utils.FileHelper
import groovy.util.logging.Slf4j
import org.apache.commons.io.FilenameUtils

/**
 * Finds the the command to be executed by the application.
 */
@Slf4j
class CommandFinder {

    /**
     * The path to main sources.
     */
    private final static String GROOVY_SOURCE_PATH = "src/main/groovy/"

    /**
     * The root package to search for commands.
     */
    private final static String ROOT_DIR = "${GROOVY_SOURCE_PATH}com/sysgears/seleniumbundle"

    /**
     * Project properties.
     */
    private final Config conf

    /**
     * Creates an instance of the command finder.
     *
     * @param conf project properties
     */
    CommandFinder(Config conf) {
        this.conf = conf
    }

    /**
     * Finds a command by a given command name. The command has to extend abstract class {@link AbstractCommand}.
     *
     * @param commandArgs command arguments objects that contains a name and arguments of the command
     *
     * @return a command instance object, that has been found, initialized with the given arguments
     *
     * @throws IllegalArgumentException if command has not been found
     */
    ICommand find(CommandArgs commandArgs) throws IllegalArgumentException {
        def command = FileHelper.getFiles(ROOT_DIR, "groovy").findAll {
            it.path.matches(/^(\w*${File.separator})*commands(${File.separator}\w*)*Command\.groovy$/)
        }.findAll {
            getCommandName(it).toLowerCase() == commandArgs.name.toLowerCase()
        }.findResult {
            def clazz = Class.forName(getClassName(it.path))

            (clazz.getSuperclass() == AbstractCommand) ? clazz : null
        }?.newInstance(commandArgs.arguments, conf) as ICommand

        command ?: {
            log.error("Command [$commandArgs.name] wasn't found.")
            throw new IllegalArgumentException("Command [$commandArgs.name] wasn't found.")
        }()
    }

    private String getCommandName(File command) {
        (command.path - "Command.groovy").split(File.separator).last()
    }

    private String getClassName(String filePath) {
        (filePath - FilenameUtils.separatorsToSystem(GROOVY_SOURCE_PATH) - ".groovy")
                .split(File.separator).join(".")
    }
}
