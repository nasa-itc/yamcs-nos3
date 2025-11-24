package org.yamcs.nos3;

import org.yamcs.YConfiguration;
import org.yamcs.commanding.PreparedCommand;
import org.yamcs.tctm.CommandPostprocessor;
import org.yamcs.logging.Log;

import java.io.*;

public class FPrimeCommandPostprocessor implements CommandPostprocessor {
    
    private static final Log log = new Log(FPrimeCommandPostprocessor.class);
    
    private String targetCommand;
    private String scriptPath;
    
    public FPrimeCommandPostprocessor(String yamcsInstance, YConfiguration config) {
        // Read configuration parameters
        this.targetCommand = config.getString("targetCommand", "CMD_NO_OP");
        this.scriptPath = config.getString("scriptPath", "./fprimecmding.sh");
        
        log.info("Initialized FPrimeCommandPostprocessor - Target Command: {}, Script: {}", 
                targetCommand, scriptPath);
    }
    
    @Override
    public byte[] process(PreparedCommand preparedCommand) {
        try {
            // Get command information
            String commandName = preparedCommand.getCmdName();
            
            log.debug("Processing outgoing command: {}", commandName);
            
            // Check if this is the command we want to intercept
            if (commandName != null && (commandName.equals(targetCommand) || commandName.contains(targetCommand))) {
                log.info("Intercepted target command: {}, executing fprimecmding.sh", commandName);
                executeFPrimeCmdingScript(commandName);
            }
            
        } catch (Exception e) {
            log.error("Error processing outgoing command", e);
        }
        
        // Return the original command binary data unchanged
        return preparedCommand.getBinary();
    }
    
    private void executeFPrimeCmdingScript(String commandName) {
        try {
            log.info("Executing fprimecmding.sh script for command: {}", commandName);
            
            ProcessBuilder processBuilder = new ProcessBuilder("bash", scriptPath);
            processBuilder.directory(new File("."));
            
            Process process = processBuilder.start();
            
            // Read all output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            String line;
            
            // Log stdout immediately
            while ((line = reader.readLine()) != null) {
                log.info("fprimecmding.sh: {}", line);
            }
            
            // Log stderr immediately  
            while ((line = errorReader.readLine()) != null) {
                log.error("fprimecmding.sh error: {}", line);
            }
            
            int exitCode = process.waitFor();
            log.info("fprimecmding.sh completed with exit code: {}", exitCode);
            
            reader.close();
            errorReader.close();
            
        } catch (IOException | InterruptedException e) {
            log.error("Failed to execute fprimecmding.sh script", e);
        }
    }
    
}