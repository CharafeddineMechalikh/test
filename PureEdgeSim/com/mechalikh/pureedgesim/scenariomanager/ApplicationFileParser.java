package com.mechalikh.pureedgesim.scenariomanager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.taskgenerator.Application;
import com.mechalikh.pureedgesim.taskgenerator.SubTask;

public class ApplicationFileParser extends XmlFileParser {

	public ApplicationFileParser(String file) {
		super(file);
	}

	@Override
	public boolean parse() {
		return checkAppFile();
	}

	protected boolean checkAppFile() {
		String condition = "> 0. Check the \"";
		String application = "\" application in \"";
		SimLog.println("%s - Checking applications file.", this.getClass().getSimpleName());
		SimulationParameters.applicationList = new ArrayList<>();
		Document doc;
		try (InputStream applicationFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(applicationFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttribtuePresent(appElement, "name");

				for (String element : List.of("type", "latency", "usage_percentage", "container_size", "request_size",
						"results_size", "task_length", "rate"))
					isElementPresent(appElement, element);

				// The generation rate (tasks per minute)
				int rate = (int) assertDouble(appElement, "rate", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The percentage of devices using this type of applications.
				int usagePercentage = (int) assertDouble(appElement, "usage_percentage", value -> (value > 0),
						condition + appElement.getAttribute("name") + application + file);

				// The type of application.
				String type = appElement.getElementsByTagName("type").item(0).getTextContent();

				// Save applications parameters.
				Application app = new Application(type, rate, usagePercentage);
				
				
				NodeList taskList = doc.getElementsByTagName("task");
				for (int i1 = 0; i1 < taskList.getLength(); i1++) {
					Node taskNode = taskList.item(i1);
					Element taskElement = (Element) taskNode;
					
					// Latency-sensitivity in seconds.
					int id =  Integer.parseInt(taskElement.getElementsByTagName("id").item(0).getTextContent());

					// Latency-sensitivity in seconds.
					String[] requirements =  taskElement.getElementsByTagName("require").item(0).getTextContent().split(",");
					
					// Latency-sensitivity in seconds.
					double latency = assertDouble(taskElement, "latency", value -> (value > 0),
							condition + taskElement.getAttribute("latency") + application + file + "\" file");

					// The size of the container (bits).
					long containerSize = (long) (8000 * assertDouble(taskElement, "container_size", value -> (value > 0),
							condition + taskElement.getAttribute("container_size") + application + file));

					// Average request size (bits).
					long requestSize = (long) (8000 * assertDouble(taskElement, "request_size", value -> (value > 0),
							condition + taskElement.getAttribute("request_size") + application + file));

					// Average downloaded results size (bits).
					long resultsSize = (long) (8000 * assertDouble(taskElement, "results_size", value -> (value > 0),
							condition + taskElement.getAttribute("results_size") + application + file));

					// Average task length (MI).
					double taskLength = assertDouble(taskElement, "task_length", value -> (value > 0),
							condition + taskElement.getAttribute("task_length") + application + file);

					SubTask subTask=new SubTask(id);
					subTask.setRequirements(requirements);
					subTask.setMaxLatency(latency);
					subTask.setContainerSizeInBits(containerSize);
					subTask.setFileSizeInBits(requestSize);
					subTask.setOutputSizeInBits(resultsSize);
					subTask.setLength(taskLength);
					app.getSubTasks().add(subTask);
				}
				
				
				SimulationParameters.applicationList.add(app);
			}

		} catch (Exception e) {
			SimLog.println("%s - Applications XML file cannot be parsed!", this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}

		SimLog.println("%s - Applications XML file successfully loaded!", this.getClass().getSimpleName());
		return true;
	}

}
