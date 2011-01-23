// Copyright 2011 Lawrence Kesteloot

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lejos.nxt.Motor;
import lejos.nxt.remote.RemoteMotor;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONValue;

/**
 * Handles requests to the API URL for controlling the brick.
 */
public class ApiServlet extends HttpServlet {
    @Override // HttpServlet
    @SuppressWarnings("unchecked") // Object to Map cast.
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        // Get the full request.
        String requestString = IOUtils.toString(request.getInputStream(), "UTF-8");

        // Parse JSON.
        Object requestObject = JSONValue.parse(requestString);
        System.out.println("Got JSON request " + requestObject);
        if (!(requestObject instanceof Map)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Map<String,Object> requestMap = (Map<String,Object>) requestObject;

        String command = (String) requestMap.get("command");
        String motorLetter = (String) requestMap.get("motor");
        System.out.println("Command is " + command + " on motor " + motorLetter);

        RemoteMotor motor;
        if (motorLetter.equals("A")) {
            motor = Motor.A;
        } else if (motorLetter.equals("B")) {
            motor = Motor.B;
        } else {
            motor = Motor.C;
        }

        if (command.equals("forward")) {
            motor.forward();
        } else if (command.equals("stop")) {
            motor.stop();
        } else if (command.equals("backward")) {
            motor.backward();
        } else if (command.equals("float")) {
            motor.flt();
        } else if (command.equals("setSpeed")) {
            int speed = ((Number) requestMap.get("speed")).intValue();
            // motor.regulateSpeed(true);
            motor.setSpeed(speed);
            System.out.println("Setting speed to " + speed);
        }

        Map<String,Object> responseMap = new HashMap<String,Object>();

        // Write headers.
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        // Serialize using JSON.
        String responseString = JSONValue.toJSONString(responseMap);
        IOUtils.write(responseString, response.getOutputStream(), "UTF-8");
    }
}
