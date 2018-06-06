/*
 *
 *   Copyright IBM Corp. 2018
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package j2serulesession;

import static j2serulesession.MessageCode.SAMPLE_ERROR_INVALID_RULESET_PATH;
import static j2serulesession.MessageCode.SAMPLE_ERROR_MISSING_RULESET_PATH;
import static j2serulesession.MessageCode.SAMPLE_FOOTER;
import static j2serulesession.MessageCode.SAMPLE_FOOTER_TAB;
import static j2serulesession.MessageCode.SAMPLE_RULEAPP;
import static j2serulesession.MessageCode.SAMPLE_RULEAPP_DESCRIPTION;
import static j2serulesession.MessageCode.SAMPLE_RULESET_PATH;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ilog.rules.res.model.IlrFormatException;
import ilog.rules.res.model.IlrPath;


public class Sample {

    private static final MessageFormatter formatter = new MessageFormatter();

    private static String RULEAPP = formatter.getMessage(SAMPLE_RULEAPP);

    private enum SampleOption {
        RuleApp(
                "r", // No_i18n
                "ruleApp", // No_i18n
                RULEAPP,
                formatter.getMessage(SAMPLE_RULEAPP_DESCRIPTION),
                null,
                true,
                false);

        private final Option option;

        private final String defaultValue;

        SampleOption(String shortName, String longName, String argumentName, String description, String defaultValue,
                boolean hasArgument, boolean required) {
            this.option = Option.builder(shortName).argName(argumentName).longOpt(longName).desc(description)
                    .hasArg(hasArgument).required(required).build();
            this.defaultValue = defaultValue;
        }

        public Option getOption() {
            return option;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    private static final Options OPTIONS = new Options();

    static {
        for (SampleOption sampleOption : SampleOption.values()) {
            OPTIONS.addOption(sampleOption.getOption());
        }
    }

    /**
     * @param args
     */
    public static void main(String... arguments) throws Exception {
        Sample sample = new Sample();
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(OPTIONS, arguments);
            IlrPath rulesetPath = sample.getRulesetPath(commandLine, arguments);
            String ruleAppArchive = sample.getRuleAppArchive(commandLine);
            RESJSEExecution execution = new RESJSEExecution();
            try {
                // Fetch the ruleset matching rulesetPath from the RES Console and execute it if any
                execution.loadRuleApp(ruleAppArchive);
                execution.executeRuleset(rulesetPath);
            } finally {
                execution.release();
            }
        } catch (ParseException | IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            sample.exitWithUsageMessage(OPTIONS);
        } catch (Throwable exception) {
            System.err.println(exception.getMessage());
            System.exit(2);
        }

    }

    private String getRuleAppArchive(CommandLine commandLine) {
        SampleOption option = SampleOption.RuleApp;
        String optionValue = commandLine.getOptionValue(option.getOption().getOpt());
        return (optionValue == null) ? option.getDefaultValue() : optionValue;
    }

    private IlrPath getRulesetPath(CommandLine commandLine, String[] arguments) throws IllegalArgumentException {
        String rulesetPathArgumentAsString = getMandatoryRulesetPathArgument(commandLine, arguments);
        if (rulesetPathArgumentAsString == null) {
            String errorMessage = getMessage(SAMPLE_ERROR_MISSING_RULESET_PATH, getMessage(SAMPLE_RULESET_PATH));
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            return IlrPath.parsePath(rulesetPathArgumentAsString);
        } catch (IlrFormatException exception) {
            String errorMessage = getMessage(SAMPLE_ERROR_INVALID_RULESET_PATH, rulesetPathArgumentAsString);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String getMessage(String key, Object... arguments) {
        return formatter.getMessage(key, arguments);
    }

    private void exitWithUsageMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        String footer_tab = getMessage(SAMPLE_FOOTER_TAB);
        String rulesetPath = getMessage(SAMPLE_RULESET_PATH);
        String footer = getMessage(SAMPLE_FOOTER, rulesetPath, footer_tab);
        String commandLineSyntax = Sample.class.getName() + " [-j <" + RULEAPP + ">] <" + rulesetPath + ">";
        formatter.printHelp(120, commandLineSyntax, null, options, footer);
        System.exit(1);
    }

    private String getMandatoryRulesetPathArgument(CommandLine commandLine, String[] arguments) {
        int nbOfArguments = arguments.length;
        if (nbOfArguments != 0) {
            List<String> unprocessedArguments = Arrays.asList(commandLine.getArgs());
            if (!unprocessedArguments.isEmpty()) {
                String rulesetPathArgumentAsString = arguments[nbOfArguments - 1];
                if (unprocessedArguments.contains(rulesetPathArgumentAsString)) {
                    return rulesetPathArgumentAsString;
                }
            }
        }
        return null;
    }
}
