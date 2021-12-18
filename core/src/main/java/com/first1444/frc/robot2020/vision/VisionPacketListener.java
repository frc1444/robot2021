package com.first1444.frc.robot2020.vision;

import com.first1444.sim.api.Clock;
import com.first1444.sim.api.surroundings.Surrounding;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.util.List;

public class VisionPacketListener implements VisionProvider, AutoCloseable {
    private final Clock clock;
    private final VisionPacketParser parser;
    private final String address;
    private final Thread thread;
    private VisionInstant instant = null;
    public VisionPacketListener(Clock clock, VisionPacketParser parser, String address){
        this.clock = clock;
        this.parser = parser;
        this.address = address;
        Thread thread = new Thread(this::run);
        thread.setDaemon(true);
        this.thread = thread;
    }

    public void start(){
        thread.start();
    }

    @Override
    public void close() {
        thread.interrupt();
    }

    @Override
    public VisionInstant getVisionInstant() {
        synchronized (this) {
            return this.instant;
        }
    }

    private void run() {
        ZContext context = new ZContext();
        try {
            try (ZMQ.Socket socket = context.createSocket(SocketType.SUB)) {
                socket.connect(address);
                socket.setLinger(0);
                socket.subscribe("".getBytes());

                while (!Thread.currentThread().isInterrupted()) {
                    final String reply;
                    try {
                        reply = socket.recvStr(0);
                    } catch (ZMQException ex) {
                        System.err.println("Got ZMQException. Hopefully the program is ending now.");
                        break;
                    }
                    if (reply != null) {
                        double timestamp = clock.getTimeSeconds();
                        try {
                            List<Surrounding> surroundings = parser.parseSurroundings(timestamp, reply);
                            VisionInstant instant = new VisionInstant(surroundings, timestamp);
                            synchronized (this) {
                                this.instant = instant;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } finally {
            try {
                context.close();
            } catch (IllegalStateException ex) {
                if (ex.getMessage().equals("errno 4")) {
                    System.err.println("Expected error when closing context");
                } else {
                    ex.printStackTrace();
                }
            }
        }
    }

}
