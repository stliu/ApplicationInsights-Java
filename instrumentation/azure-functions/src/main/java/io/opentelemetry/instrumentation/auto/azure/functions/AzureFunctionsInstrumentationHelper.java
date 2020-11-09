package io.opentelemetry.instrumentation.auto.azure.functions;

import com.google.common.base.Strings;
import io.opentelemetry.instrumentation.api.aiconnectionstring.AiConnectionString;

public class AzureFunctionsInstrumentationHelper {

  public static void lazilySetConnectionString() {
    boolean lazySetOptIn = Boolean.parseBoolean(System.getProperty("LazySetOptIn"));
    String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");
    String enableAgent = System.getenv("APPLICATIONINSIGHTS_ENABLE_AGENT");
    // race condition (two initial requests happening at the same time) is not a worry here
    // because at worst they both enter the condition below and update the connection string
    if (!AiConnectionString.hasConnectionString()) {
      if (!Strings.isNullOrEmpty(connectionString)) {
        setConnectionString(lazySetOptIn, enableAgent, connectionString);
      }
    } else {
      // if the instrumentation key is neither null nor empty , we will create a default
      // connection string based on the instrumentation key.
      // this is to support Azure Functions that were created prior to the introduction of
      // connection strings
      String instrumentationKey = System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY");
      if (!Strings.isNullOrEmpty(instrumentationKey)) {
        setConnectionString(lazySetOptIn, enableAgent, instrumentationKey);
      }
    }
  }

  private static void setConnectionString(boolean lazySetOptIn, String enableAgent, String value) {
    if (lazySetOptIn) {
      // when LazySetOptIn is on, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is null or true
      if (enableAgent == null || Boolean.parseBoolean(enableAgent)) {
        AiConnectionString.setConnectionString(value);
      }
    } else {
      // when LazySetOptIn is off, enable agent only when APPLICATIONINSIGHTS_ENABLE_AGENT is true
      if (Boolean.parseBoolean(enableAgent)) {
        AiConnectionString.setConnectionString(value);
      }
    }
  }
}
