package eu.qualimaster.comserver.adaptation;

import eu.qualimaster.adaptation.external.AlgorithmChangedMessage;
import eu.qualimaster.adaptation.external.ChangeParameterRequest;
import eu.qualimaster.adaptation.external.DisconnectRequest;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.HardwareAliveMessage;
import eu.qualimaster.adaptation.external.IDispatcher;
import eu.qualimaster.adaptation.external.LoggingFilterRequest;
import eu.qualimaster.adaptation.external.LoggingMessage;
import eu.qualimaster.adaptation.external.MonitoringDataMessage;
import eu.qualimaster.adaptation.external.PipelineMessage;
import eu.qualimaster.adaptation.external.ResponseMessage;
import eu.qualimaster.adaptation.external.SwitchAlgorithmRequest;
import eu.qualimaster.adaptation.external.UsualMessage;
import eu.qualimaster.events.ResponseStore;

import java.io.PrintWriter;

/**
 * Created by ap0n on 26/1/2016.
 */
public class Dispatcher implements IDispatcher {

  private PrintWriter printWriter;
  private ResponseStore<UsualMessage, ChangeParameterRequest, ResponseMessage> responseStore;

  public Dispatcher(PrintWriter printWriter,
                    ResponseStore<UsualMessage, ChangeParameterRequest,
                        ResponseMessage> responseStore) {
    this.printWriter = printWriter;
    this.responseStore = responseStore;
  }

  @Override
  public void handleDisconnectRequest(DisconnectRequest disconnectRequest) {
  }

  @Override
  public void handleSwitchAlgorithmRequest(SwitchAlgorithmRequest switchAlgorithmRequest) {
  }

  @Override
  public void handleChangeParameterRequest(ChangeParameterRequest<?> changeParameterRequest) {
  }

  @Override
  public void handleMonitoringDataMessage(MonitoringDataMessage monitoringDataMessage) {
  }

  @Override
  public void handleAlgorithmChangedMessage(AlgorithmChangedMessage algorithmChangedMessage) {
  }

  @Override
  public void handleHardwareAliveMessage(HardwareAliveMessage hardwareAliveMessage) {
  }

  @Override
  public void handlePipelineMessage(PipelineMessage pipelineMessage) {
  }

  @Override
  public void handleLoggingMessage(LoggingMessage loggingMessage) {
  }

  @Override
  public void handleLoggingFilterRequest(LoggingFilterRequest loggingFilterRequest) {
  }

  @Override
  public void handleExecutionResponseMessage(ExecutionResponseMessage executionResponseMessage) {
    synchronized (printWriter) {

      ChangeParameterRequest req = responseStore.receivedEvent(executionResponseMessage);
      String param = req.getParameter();
      String value = req.getValue().toString();
      String result = executionResponseMessage.getResult().toString();

      if (param.equals("playerList")) {
        String[] r = value.split("/");
        String v;
        if (value.startsWith("add")) {
          v = ",added,";
        } else {
          v = ",removed,";
        }
        printWriter.println(r[0] + "_response," + result + v + r[1]);
      } else {
        printWriter.println("change" + param + "_response," + result + ",newValue," + value);
      }

    }
  }
}
