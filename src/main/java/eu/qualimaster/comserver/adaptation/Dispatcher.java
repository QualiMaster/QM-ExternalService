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
import eu.qualimaster.adaptation.external.SwitchAlgorithmRequest;

/**
 * Created by ap0n on 26/1/2016.
 */
public class Dispatcher implements IDispatcher {

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

  }
}
