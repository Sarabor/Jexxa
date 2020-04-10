package io.ddd.jexxa.infrastructure.drivingadapter.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import io.ddd.jexxa.core.JexxaMain;
import io.ddd.jexxa.utils.JexxaLogger;
import org.junit.Assert;
import org.junit.Test;

public class JMSAdapterTest
{
    @Test(timeout = 10000)
    public void startJMSAdapter() 
    {
        //Arrange
        var messageListener = new MyListener();
        var properties = new Properties();
        properties.put(JMSAdapter.JNDI_FACTORY_KEY, JMSAdapter.DEFAULT_JNDI_FACTORY);
        properties.put(JMSAdapter.JNDI_PROVIDER_URL_KEY, JMSAdapter.DEFAULT_JNDI_PROVIDER_URL);
        properties.put(JMSAdapter.JNDI_USER_KEY, JMSAdapter.DEFAULT_JNDI_USER);
        properties.put(JMSAdapter.JNDI_PASSWORD_KEY, JMSAdapter.DEFAULT_JNDI_PASSWORD);
        var objectUnderTest = new JMSAdapter(properties);
        objectUnderTest.register(messageListener);
        MyProducer myProducer = new MyProducer(properties);
        
        //Act
        objectUnderTest.start();
        myProducer.sendToTopic();


        //Assert 
        while (messageListener.getMessages().isEmpty())
        {
            Thread.onSpinWait();
        }

        objectUnderTest.stop();
    }



    @Test(timeout = 10000)
    public void startJMSAdapterJexxa() 
    {
        //Arrange
        var messageListener = new MyListener();
        var properties = new Properties();
        properties.put(JMSAdapter.JNDI_FACTORY_KEY, JMSAdapter.DEFAULT_JNDI_FACTORY);
        properties.put(JMSAdapter.JNDI_PROVIDER_URL_KEY, JMSAdapter.DEFAULT_JNDI_PROVIDER_URL);
        properties.put(JMSAdapter.JNDI_USER_KEY, JMSAdapter.DEFAULT_JNDI_USER);
        properties.put(JMSAdapter.JNDI_PASSWORD_KEY, JMSAdapter.DEFAULT_JNDI_PASSWORD);

        JexxaMain jexxaMain = new JexxaMain("JMSAdapterTest", properties);
        jexxaMain.bind(JMSAdapter.class, messageListener);
        MyProducer myProducer = new MyProducer(properties);

        //Act
        jexxaMain.startDrivingAdapters();
        myProducer.sendToTopic();

        //Assert
        while (messageListener.getMessages().isEmpty())
        {
            Thread.onSpinWait();
        }

        jexxaMain.stopDrivingAdapters();
    }



    static public class MyListener implements MessageListener
    {

        List<Message> messageList = new ArrayList<>();

        @Override
        @JMSListener(destination = "MyListener", messagingType = JMSListener.MessagingType.Topic)
        public void onMessage(Message message)
        {
            try
            {
                JexxaLogger.getLogger(MyListener.class).info(((TextMessage) message).getText());
                messageList.add(message);
            }
            catch ( Exception e) {
                JexxaLogger.getLogger(MyListener.class).error(e.getMessage());
            }
        }

        public List<Message> getMessages()
        {
            return messageList;
        }
    }

    public static class MyProducer {
        Connection connection;
        MyProducer(Properties properties)
        {
            JMSAdapter jmsAdapter = new JMSAdapter(properties);
            this.connection = jmsAdapter.createConnection();
        }
        public void sendToTopic() {
            try {
                connection.start();

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic(MyListener.class.getSimpleName());

                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                String text = "Hello world" ;
                TextMessage message = session.createTextMessage(text);

                producer.send(message);

                session.close();
                connection.close();
            }
            catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }
    }

}