package com.github.ROS_E_Stop;

/**
 * Created by levi on 2/22/17.
 */

import java.lang.String;

public class RosTopicChecker {
    public static boolean isValidTopicName(String topic)
    {
        for(int i = 0; i < topic.length(); i++)
        {
            if(i == 0)  //check first character, must be /, alpha character, or ~
            {
                if(topic.charAt(i) != '/' && topic.charAt(i) != '~' && !isAplhaChar(topic.charAt(i)))
                {
                    return false;   //topic not valid
                }
            }
            else    //all remaining characters must be alphanumeric, /, or _
            {
                if(topic.charAt(i) != '/' && topic.charAt(i) != '_' && !isAplhaCharOrDigit(topic.charAt(i)))
                {
                    return false;   //topic not valid
                }
            }
        }
        return true;
    }

    private static boolean isAplhaChar(char c)
    {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z');
    }

    private static boolean isAplhaCharOrDigit(char c)
    {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9');
    }
}
