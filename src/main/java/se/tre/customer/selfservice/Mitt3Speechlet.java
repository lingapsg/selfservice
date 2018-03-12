package se.tre.customer.selfservice;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Calendar;

import static com.amazon.speech.speechlet.SpeechletResponse.newAskResponse;
import static com.amazon.speech.speechlet.SpeechletResponse.newTellResponse;

public class Mitt3Speechlet implements SpeechletV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mitt3Speechlet.class);

    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        LOGGER.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
    }

    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        LOGGER.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        return getWelcomeResponse();
    }

    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();
        LOGGER.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
        Intent intent = request.getIntent();
        String intentName = intent.getName();
        LOGGER.info("intent name :" + intentName);
        if ("GetInvoiceIntent".equalsIgnoreCase(intentName)) {
            session.setAttribute("previousIntent", intentName);
            return getInvoiceResponse(session, intent);
        } else if ("GetDataIntent".equalsIgnoreCase(intentName)) {
            session.setAttribute("previousIntent", intentName);
            return getDataResponse(session, intent);
        } else if ("mobileNoIntent".equalsIgnoreCase(intentName)) {
            String previousIntent = (String)session.getAttribute("previousIntent");
            if (validateMsisdn(session, intent)) {
                return newAskResponse("Please provide your mobile number", "Please provide your mobile number");
            } else if (previousIntent.equalsIgnoreCase("GetInvoiceIntent")) {
                return getInvoiceResponse(session, intent);
            } else if (previousIntent.equalsIgnoreCase("GetDataIntent")) {
                return getDataResponse(session, intent);
            }
        }
        return newAskResponse("Hey I can help you to get your invoice amount", "Hey I can help you to get your invoice amount");
    }

    private SpeechletResponse getDataResponse(Session session, Intent intent) {
        if (validateMsisdn(session, intent))
            return newAskResponse("Please provide your mobile number", "Please provide your mobile number");
        if (intent.getSlot("month") != null && intent.getSlot("month").getValue() == null) {
            return getMonthsDataResponse(ValidationUtil.getMonthByIndex(Calendar.getInstance().get(Calendar.MONTH)));
        } else {
            Slot monthSlot = intent.getSlot("month");
            if (monthSlot != null && monthSlot.getValue() != null) {
                return getMonthsDataResponse(monthSlot.getValue());
            }
        }
        return newAskResponse("Hey I can help you to get your data usage", "Hey I can help you to get your data usage");
    }

    private SpeechletResponse getMonthsDataResponse(String month) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText("You have 300 MB data left for the month of " + ValidationUtil.getMonthByOthers(month));
        return newTellResponse(outputSpeech);
    }

    private SpeechletResponse getInvoiceResponse(Session session, Intent intent) {
        if (validateMsisdn(session, intent))
            return newAskResponse("Please provide your mobile number", "Please provide your mobile number");
        Slot monthSlot = intent.getSlot("month");
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        if (monthSlot != null && monthSlot.getValue() != null) {
            LOGGER.info(monthSlot.getValue());
            if (!ValidationUtil.isValidMonth(monthSlot.getValue())) {
                outputSpeech.setText("I can't find invoice for the given month. Please provide a valid month");
                return newTellResponse(outputSpeech);
            }
            outputSpeech.setText("Your invoice amount is 300 SEK for the month of " + monthSlot.getValue());
            return newTellResponse(outputSpeech);
        } else {
            return newAskResponse("Which month's invoice amount you are looking for ?",
                    "Which month's invoice amount you are looking for ?");
        }

    }

    private boolean validateMsisdn(Session session, Intent intent) {
        Slot msisdnSlot = intent.getSlot("msisdn");
        if (msisdnSlot != null && msisdnSlot.getValue() != null) {
            session.setAttribute("msisdn", msisdnSlot.getValue());
        }
        return StringUtils.isEmpty((String) session.getAttribute("msisdn"));
    }

    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        LOGGER.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        requestEnvelope.getSession().removeAttribute("msisdn");
    }

    private SpeechletResponse getWelcomeResponse() {
        String speechOutput = "<speak>"
                + "Welcome to Mitt3. </speak>";
        String repromptText =
                "I can help you to get your invoice amount. Please provide your mobile number";

        return newAskResponse(speechOutput, true, repromptText, false);
    }

    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        return newAskResponse(stringOutput, false, repromptText, false);
    }

    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
                                             String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(stringOutput);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }
}
