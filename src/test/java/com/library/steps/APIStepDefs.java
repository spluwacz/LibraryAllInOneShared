package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.DB_Util;
import com.library.utility.DatabaseHelper;
import com.library.utility.LibraryAPI_Util;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class APIStepDefs {



    /********** US01 **********/

    //Global variables:
    RequestSpecification givenPart = RestAssured.given().log().uri();
    Response response;
    ValidatableResponse thenPart;
    JsonPath jp;

    String expectedID;

    Map<String, Object> randomData=new HashMap<>();

    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String role) {
        //OPT1
        //givenPart.header("x-library-token", LibraryAPI_Util.getToken("librarian10@library","libraryUser"));

        //OPT2
        //givenPart.header("x-library-token", LibraryAPI_Util.getToken(ConfigurationReader.getProperty("librarian_username"),ConfigurationReader.getProperty("librarian_password")));

        //OPT3 -->Go to LibraryAPI_Util line 45 --> getToken(userType) Method
        givenPart.header("x-library-token", LibraryAPI_Util.getToken(role));

        givenPart.log().all();
    }

    @Given("Accept header is {string}")
    public void accept_header_is(String acceptHeader) {
        givenPart.accept(acceptHeader);

    }

    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {

        //it has already declared globally:
        response = givenPart.when().get(endpoint);

        //We will need jsonPath object at next steps:
        jp = response.jsonPath();

    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expectedStatusCode) {
        // OPT - 1
        Assert.assertEquals(expectedStatusCode, response.statusCode());

        // OPT - 2
        thenPart = response.then();

        thenPart.statusCode(expectedStatusCode);


    }

    @Then("Response Content type is {string}")
    public void response_content_type_is(String expectedContentType) {

        // OPT - 1
        Assert.assertEquals(expectedContentType, response.contentType());

        // OPT - 2
        thenPart.contentType(expectedContentType);
    }

    @Then("Each {string} field should not be null")
    public void each_field_should_not_be_null(String path) {

        // OPT - 1
        thenPart.body(path, Matchers.everyItem(notNullValue()));

        // OPT - 2
        List<String> allData = jp.getList(path);
        for (String eachData : allData) {
            Assert.assertNotNull(eachData);
        }
    }

    /********** US02 **********/


    @Given("Path param {string} is {string}")
    public void path_param_is(String pathParam, String value) {
        givenPart.pathParam(pathParam, value);
        expectedID = value; //declare globally
    }

    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String path) {
        String actualID = jp.getString(path);
        Assert.assertEquals(expectedID, actualID);
    }

    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> allPaths) {

        for (String eachPath : allPaths) {
            thenPart.body(eachPath, notNullValue());
        }

    }

    /********** US03 - 1 **********/

    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String contentType) {

        givenPart.contentType(contentType);
    }

    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String dataType) {



        switch (dataType) {
            case "book":
                //Map<String, Object> randomBook=LibraryAPI_Util.getRandomBookMap();
                randomData= LibraryAPI_Util.getRandomBookMap();
                break;
            case "user":
                randomData = LibraryAPI_Util.getRandomUserMap();

                break;
            default:
                throw new RuntimeException("Invalid Data Type " + dataType);
        }


        givenPart.formParams(randomData);

    }

    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {

        response = givenPart.when().post(endpoint);
        thenPart = response.then();
        jp=response.jsonPath();

    }

    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String path, String expectedMessage) {
        String actualMessage = jp.getString(path);

        Assert.assertEquals(expectedMessage,actualMessage);
    }

    @Then("{string} field should not be null")
    public void field_should_not_be_null(String path) {
        // OPT 1
        thenPart.body(path,Matchers.notNullValue());

        // OPT 2
        Assert.assertNotNull(jp.getString(path));
    }

    /********** US03 - 2 **********/

    LoginPage loginPage=new LoginPage();

    BookPage bookPage=new BookPage();


    @Given("I logged in Library UI as {string}")
    public void i_logged_in_library_ui_as(String role) {
        loginPage.login(role);
    }

    @Given("I navigate to {string} page")
    public void i_navigate_to_page(String module) {
        bookPage.navigateModule(module);
    }



    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() throws SQLException {

        // API - EXPECTED - GET FROM REQUEST BODY
        System.out.println("API = " + randomData);

        // DB  - ACTUAL - WRITE QUERY BY USING book_id
        String bookId = jp.getString("book_id");

        // how to create query in a different way
        String query= DatabaseHelper.getBookByIdQuery(bookId);
        DB_Util.runQuery(query);

        Map<String, Object> dbMap = DB_Util.getRowMap(1);
        dbMap.remove("id");
        dbMap.remove("added_date");

        Assert.assertEquals(randomData,dbMap);


        // UI  - ACTUAL - OPEN UI GET CORRESPONDING FIELD DATA
        String bookName = (String)randomData.get("name");

        bookPage.search.sendKeys(bookName);
        BrowserUtil.waitFor(3);

        bookPage.editBook(bookName).click();
        BrowserUtil.waitFor(3);

        Map<String, Object> uiMap = new LinkedHashMap<>();
        String uiBookName = bookPage.bookName.getAttribute("value");
        uiMap.put("name",uiBookName);

        String uiISBN = bookPage.isbn.getAttribute("value");
        uiMap.put("isbn",uiISBN);

        String uiYear=bookPage.year.getAttribute("value");
        uiMap.put("year",uiYear);

        String uiAuthor=bookPage.author.getAttribute("value");
        uiMap.put("author",uiAuthor);

        String uiDesc=bookPage.description.getAttribute("value");
        uiMap.put("description",uiDesc);

        // Get Book Category id
        // Get book category name from UI
        String selectedCategory = BrowserUtil.getSelectedOption(bookPage.categoryDropdown);
        System.out.println("selectedCategory = " + selectedCategory);

        String query2 = DatabaseHelper.getCategoryIdQuery(selectedCategory);
        DB_Util.runQuery(query2);


        String uiCategoryID = DB_Util.getFirstRowFirstColumn();
        uiMap.put("book_category_id",uiCategoryID);

        Assert.assertEquals(randomData,uiMap);

    }


    /********** US04 - 1 **********/
    //No need any steps

    /********** US04 - 2 **********/

    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {

        int id = jp.getInt("user_id");

        String query="select full_name,email,user_group_id,status,start_date,end_date,address " +
                "from users where id="+id;

        DB_Util.runQuery(query);


        Map<String, Object> actualData = DB_Util.getRowMap(1);

        // Expected --> API --> randomData --> map

        String password= (String) randomData.remove("password");


        Assert.assertEquals(randomData,actualData);

        // Add password into randomData
        randomData.put("password",password);

        System.out.println("randomData = " + randomData);
        System.out.println("actualData = " + actualData);

    }

    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() throws InterruptedException {

        LoginPage loginPage=new LoginPage();

        String email = (String) randomData.get("email");

        String password = (String) randomData.get("password");

        loginPage.login(email,password);

        BookPage bookPage=new BookPage();
        BrowserUtil.waitForVisibility(bookPage.accountHolderName,15);

    }
    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {

        BookPage bookPage=new BookPage();

        String uiFullName = bookPage.accountHolderName.getText();

        String apiFullName = (String) randomData.get("full_name");

        Assert.assertEquals(apiFullName,uiFullName);

    }

    /********** US05 **********/

    String token;
    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {
        token = LibraryAPI_Util.getToken(email, password);
    }

    @Given("I send {string} information as request body")
    public void i_send_token_information_as_request_body(String key) {
        givenPart.formParam(key,token);
    }



}
