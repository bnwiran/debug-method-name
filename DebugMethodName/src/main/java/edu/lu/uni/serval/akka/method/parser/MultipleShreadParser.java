package edu.lu.uni.serval.akka.method.parser;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class MultipleShreadParser {
	
	private final ProjectsMessage msg;
	private final int numberOfWorkers;

	public MultipleShreadParser(String project, String outputPath, int numberOfWorkers) {
		this.msg = new ProjectsMessage(project, 0, outputPath);
		this.numberOfWorkers = numberOfWorkers;
	}

	public void parseMethods() {
		ActorSystem system = null;
		
		try {
			system = ActorSystem.create("Parsing-Method-System");
			ActorRef parsingActor = system.actorOf(ParseProjectActor.props(numberOfWorkers), "parse-method-actor");
			parsingActor.tell(msg, ActorRef.noSender());
		} catch (Exception e) {
			system.terminate();
			e.printStackTrace();
		}
	}
}
