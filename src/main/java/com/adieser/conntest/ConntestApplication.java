package com.adieser.conntest;

import com.adieser.conntest.views.JavafxApplication;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConntestApplication {

	//public static void main(String[] args) {
	//	SpringApplication.run(ConntestApplication.class, args);
	//}

	public static void main(String[] args) {
		Application.launch(JavafxApplication.class, args);
	}

}
