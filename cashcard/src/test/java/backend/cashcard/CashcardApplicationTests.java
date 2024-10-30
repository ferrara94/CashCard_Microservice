package backend.cashcard;

import backend.cashcard.entity.CashCard;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import javax.print.Doc;git commit -m "load cashcard"


import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashcardApplicationTests {

	/**
	 * The annotation @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	 * indicates that your Spring Boot test will start an embedded web server on a random port.
	 * This allows you to test your application's web layer (e.g., controllers) without conflicts from
	 * any other running services.
	 * Using a random port is particularly useful in tests to avoid port conflicts and ensure that your
	 * tests can run in isolation. You can then access the server using the TestRestTemplate or any other HTTP client.
	 **/

	@Autowired
	TestRestTemplate restTemplate;

	/**
	 * If we run the test without implementing the rest controller we get
	 * org.opentest4j.AssertionFailedError:
	 *   expected: 200 OK
	 *   but was: 404 NOT_FOUND
	 * Since we haven't instructed Spring Web how to handle GET cashcards/99,
	 * Spring Web is automatically responding that the endpoint is NOT_FOUND.
	 * Spring WEB is handling it for us
	 **/
	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate
													.withBasicAuth("felix","abc123")
													.getForEntity("/cashcards/99",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		/**
		*This converts the response String into a JSON-aware object
		**/
		DocumentContext documentContext = JsonPath.parse(response.getBody());

		/**
		 * When we request a Cash Card with id of 99 a JSON object will
		 * be returned with something in the id field
		 **/
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity("/cashcards/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard(){
		//if you want the server to generate the ID, you should specify this in the configuration (?)
		CashCard newCashCard = new CashCard(33L, 250.00);
		ResponseEntity<Void> createResponse = restTemplate
				.withBasicAuth("felix","abc123")
				.postForEntity("/cashcards", newCashCard, Void.class);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity(locationOfNewCashCard, String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() throws JSONException {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity("/cashcards",String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.00, 150.00);
	}

	@Test
	void shouldReturnAPageOfCashCards(){
		String url = "/cashcards?page=0&size=1";
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity(url, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfCashCards(){
		/*
		* page=0: Get the first page. Page indexes start at 0.
		* size=1: Each page has size 1
		* sort=amount,desc
		* */
		String url = "/cashcards?page=0&size=1&sort=amount,desc";
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity(url, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray read = documentContext.read("$[*]");

		assertThat(read.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredential(){
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("BAD-USER","abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("felix","BAD-PASSWORD")
				.getForEntity("/cashcards", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("user-owns-no-cards", "qrs456")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn(){
		ResponseEntity<String> response = restTemplate
											.withBasicAuth("felix","abc123")
											.getForEntity("/cashcards/102",String.class); //kumar data

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard(){
		CashCard cashCardUpdate = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("felix","abc123")
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		/*
		* With no Controller endpoint, this PUT call is forbidden! Spring Security
		* automatically handled this scenario for us. Nice!
		*/

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist(){
		CashCard unkownCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unkownCard);

		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("felix","abc123")
				.exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse(){
		CashCard kumarsCard = new CashCard(null, 333.33,null );
		HttpEntity<CashCard> request = new HttpEntity<>(kumarsCard);

		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("felix","abc123")
				.exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext // annotation to all tests which change the data. If we don't, then these tests could affect the result of other tests in the file.
	void shouldDeleteAnExistingCashCard() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("felix","abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("felix","abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() {
		ResponseEntity<Void> deleteResponse = restTemplate
				.withBasicAuth("felix","abc123")
				.exchange("/cashcards/99999", HttpMethod.DELETE, null, Void.class);

		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
		ResponseEntity<Void> deleteResponse = restTemplate
				.withBasicAuth("felix","abc123")
				.exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);

		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		//Test if the record that we tried to delete still exists, since we were not authorize to do that
		ResponseEntity<String> getRespose = restTemplate
				.withBasicAuth("kumar2","xyz789")
				.getForEntity("/cashcards/102", String.class);

		assertThat(getRespose.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
