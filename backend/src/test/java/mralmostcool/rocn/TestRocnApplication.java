package mralmostcool.rocn;

import org.springframework.boot.SpringApplication;

public class TestRocnApplication {

	public static void main(String[] args) {
		SpringApplication.from(RocnApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
