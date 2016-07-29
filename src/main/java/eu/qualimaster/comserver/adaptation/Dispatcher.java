package eu.qualimaster.comserver.adaptation;

import eu.qualimaster.adaptation.external.AlgorithmChangedMessage;
import eu.qualimaster.adaptation.external.ChangeParameterRequest;
import eu.qualimaster.adaptation.external.CloudPipelineMessage;
import eu.qualimaster.adaptation.external.DisconnectRequest;
import eu.qualimaster.adaptation.external.DispatcherAdapter;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.HardwareAliveMessage;
import eu.qualimaster.adaptation.external.LoggingFilterRequest;
import eu.qualimaster.adaptation.external.LoggingMessage;
import eu.qualimaster.adaptation.external.MonitoringDataMessage;
import eu.qualimaster.adaptation.external.PipelineMessage;
import eu.qualimaster.adaptation.external.PipelineStatusRequest;
import eu.qualimaster.adaptation.external.PipelineStatusResponse;
import eu.qualimaster.adaptation.external.ReplayMessage;
import eu.qualimaster.adaptation.external.ResponseMessage;
import eu.qualimaster.adaptation.external.SwitchAlgorithmRequest;
import eu.qualimaster.adaptation.external.UpdateCloudResourceMessage;
import eu.qualimaster.adaptation.external.UsualMessage;
import eu.qualimaster.events.ResponseStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.PrintWriter;

import static eu.qualimaster.adaptation.external.ExecutionResponseMessage.ResultType.SUCCESSFUL;

/**
 * Created by ap0n on 26/1/2016.
 */
public class Dispatcher extends DispatcherAdapter {

  final static Logger logger = LoggerFactory.getLogger(Dispatcher.class);
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
      int res = executionResponseMessage.getResult() == SUCCESSFUL ? 1 : 0;

      if (param.equals("playerList")) {
        String[] r = value.split("/");
        String v;
        if (value.startsWith("add")) {
          v = ",added,";
        } else {
          v = ",removed,";
        }
        printWriter.println(r[0] + "_response," + res + v + r[1] + "!");
      } else {
        printWriter.println("change" + param + "_response," + res + ",newValue," + value + "!");
      }

      if (res == 0) {
        logger.error("Change parameter " + param + " failed. Description: "
                     + executionResponseMessage.getDescription()
                     + "message id: " + executionResponseMessage.getMessageId());
      }
    }
  }

  @Override
  public void handlePipelineStatusRequest(PipelineStatusRequest pipelineStatusRequest) {
    throw new NotImplementedException();
  }

  @Override
  public void handlePipelineStatusResponse(PipelineStatusResponse pipelineStatusResponse) {
    throw new NotImplementedException();
  }

  @Override
  public void handleUpdateCloudResourceMessage(
      UpdateCloudResourceMessage updateCloudResourceMessage) {
    throw new NotImplementedException();
  }

  @Override
  public void handleCloudPipelineMessage(CloudPipelineMessage cloudPipelineMessage) {
    throw new NotImplementedException();
  }

  @Override
  public void handleReplayMessage(ReplayMessage replayMessage) {
    throw new NotImplementedException();
  }
}
