import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

public class MovieReviewApplication {
    private static List<Movie> movies = new ArrayList<>();
    private static List<Review> reviews = new ArrayList<>();
    private static List<User> users = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    private static User currentUser = null;
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void main(String[] args) {
        initializeDatabase();
        System.out.println("Loading Mongo Dependency");
        System.out.println("Initializing connection to Mongo v4.4.0");
        System.out.println("Syncing data with MongoDB");
        loadDataFromDatabase();
        System.out.println("Sync complete\n");
//        registerShutdownHook();
//        addTestData(); // Add some test data (movies and reviews)
        registerOrLogin();
        displayMenu();
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Store any modified or new data back to the database
            // Iterate through the users list and update the users collection
            MongoCollection<Document> usersCollection = database.getCollection("Users");
            for (User user : users) {
                if (!documentExists(usersCollection, "userId", user.getUserId())) {
                    Document doc = new Document("username", user.getUsername())
                            .append("password", user.getPassword())
                            .append("email", user.getEmail())
                            .append("userId", user.getUserId()); // Updated field name to "userId"
                    usersCollection.insertOne(doc);
                }
            }

            // Iterate through the movies list and update the movies collection
            MongoCollection<Document> moviesCollection = database.getCollection("Movies");
            for (Movie movie : movies) {
                if (!documentExists(moviesCollection, "movieId", movie.getMovieId())) {
                    Document doc = new Document("title", movie.getTitle())
                            .append("description", movie.getDescription())
                            .append("releaseDate", movie.getReleaseDate())
                            .append("genre", movie.getGenre())
                            .append("movieId", movie.getMovieId());
                    moviesCollection.insertOne(doc);
                }
            }

            // Iterate through the reviews list and update the reviews collection
            MongoCollection<Document> reviewsCollection = database.getCollection("Reviews");
            for (Review review : reviews) {
                Document reviewDoc = new Document("reviewId", review.getReviewId()) // Include reviewId in the document
                        .append("userId", review.getUserId())
                        .append("movieId", review.getMovieId())
                        .append("rating", review.getRating())
                        .append("comments", review.getComments());
                reviewsCollection.updateOne(
                        Filters.eq("reviewId", review.getReviewId()), // Use reviewId for filtering
                        new Document("$set", reviewDoc),
                        new UpdateOptions().upsert(true)
                );
            }

            // Close the database connection
            if (mongoClient != null) {
                mongoClient.close();
            }
        }));
    }


    private static boolean documentExists(MongoCollection<Document> collection, String fieldName, String value) {
        Document query = new Document(fieldName, value);
        return collection.countDocuments(query) > 0;
    }

    private static void loadDataFromDatabase() {
        // Load movies
        MongoCollection<Document> moviesCollection = database.getCollection("Movies");
        try (MongoCursor<Document> cursor = moviesCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Movie movie = new Movie(doc.getString("movieId"), doc.getString("title"), doc.getString("description"),
                        doc.getString("releaseDate"), doc.getString("genre"));
                movies.add(movie);
            }
        }

        // Load users
        MongoCollection<Document> usersCollection = database.getCollection("Users");
        try (MongoCursor<Document> cursor = usersCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String userId = doc.getString("userId"); // Parse userId as string
                User user = new User(userId, doc.getString("username"), doc.getString("password"),
                        doc.getString("email"));
                users.add(user);
            }
        }

        // Load reviews
        MongoCollection<Document> reviewsCollection = database.getCollection("Reviews");
        try (MongoCursor<Document> cursor = reviewsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                // Fetch user and movie references
                String userId = doc.getString("userId"); // Parse userId as string
                User user = findUserById(userId);
                String movieId = doc.getString("movieId"); // Parse movieId as string
                Movie movie = findMovieById(movieId);
                String reviewId = doc.getString("reviewId");
                if (user != null && movie != null) {
                    // Construct the review with userId and movieId strings
                    Review review = new Review(reviewId, userId, movieId, doc.getInteger("rating"), doc.getString("comments"));
                    reviews.add(review);
                }
            }
        }
    }

    private static User findUserById(String userId) {
        for (User user : users) {
            if (user.getUserId().equals(userId)) {
                return user;
            }
        }
        return null;
    }

    private static Movie findMovieById(String movieId) {
        for (Movie movie : movies) {
            if (movie.getMovieId().equals(movieId)) {
                return movie;
            }
        }
        return null;
    }

    private static Review findReviewById(String movieId) {
        for (Review review : reviews) {
            if (review.getMovieId().equals(movieId)) {
                return review;
            }
        }
        return null;
    }


    private static void initializeDatabase() {
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("MovieReviewApplication");
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void viewAllUsers() {
        System.out.println("All Users:");
        for (User user : users) {
            System.out.println("Username: " + user.getUsername() + ", Email: " + user.getEmail());
        }
    }

    private static void removeUser() {
        System.out.println("Removing a User");
        System.out.print("Enter the username of the user you want to remove: ");
        String usernameToRemove = scanner.nextLine();

        boolean userFound = false;
        for (User user : users) {
            if (user.getUsername().equals(usernameToRemove)) {
                users.remove(user);
                System.out.println("User '" + usernameToRemove + "' removed successfully.");
                userFound = true;
                break;
            }
        }
        if (!userFound) {
            System.out.println("User not found.");
        }
    }

    private static void registerOrLogin() {
        System.out.println("Welcome to the Movie Review Application!");

        while (true) {
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.println("4. view all users");
            System.out.println("5. remove user");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline character

            switch (choice) {
                case 1:
                    registerUser();
                    break;
                case 2:
                    login();
                    if (currentUser != null) {
                        return;
                    }
                    break;
                case 3:
                    System.out.println("Saving Data...");
                    registerShutdownHook();
                    System.out.println("exiting...");
                    System.exit(0);
                case 4:
                    viewAllUsers();
                    break;
                case 5:
                    removeUser();
                    break;
                case 6:
                    getAllUserDetails();
                    break;

                case 7:
                    getAllMovieDetails();
                    break;

                case 8:
                    getAllReviewDetails();
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void registerUser() {
        System.out.println("Registration");
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        if (isUsernameTaken(username)) {
            System.out.println("Username already taken. Please choose another one.");
            return;
        }
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        int maxUserId = 0;
        for (User user : users) {
            int userIdInt = Integer.parseInt(user.getUserId());
            if (userIdInt > maxUserId) {
                maxUserId = userIdInt;
            }
        }

        // Assign new user ID by incrementing the maximum user ID found
        int newUserId = maxUserId + 1;

        User newUser = new User(Integer.toString(newUserId), username, password, email);
        users.add(newUser);
        System.out.println("Registration successful. You can now log in.");
    }



    private static boolean isUsernameTaken(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private static void login() {
        System.out.println("Login");
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                currentUser = user;
                System.out.println("Login successful. Welcome, " + currentUser.getUsername() + "!");
                return;
            }
        }
        System.out.println("Login failed. Please check your username and password.");
    }
    // Method to display the main menu
    private static void displayMenu() {
        if (currentUser == null) {
            return;
        }

        System.out.println("\nMain Menu");
        System.out.println("1. Add a Movie");
        System.out.println("2. Post a Review");
        System.out.println("3. List Movies with Reviews");
        System.out.println("4. Logout");
        System.out.println("5. update Review");
        System.out.println("6. remove a Review");
        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        switch (choice) {
            case 1:
                addMovie();
                break;
            case 2:
                postReview();
                break;
            case 3:
                listMoviesWithReviews();
                break;
            case 4:
                currentUser = null;
                registerOrLogin();
                break;
            case 5:
                updateReview();
                break;
            case 6:
                removeReview();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }

        displayMenu(); // Display the menu again
    }

    private static void addMovie() {
        System.out.println("Adding a Movie");
        System.out.print("Enter Title: ");
        String title = scanner.nextLine();
        System.out.print("Enter Description: ");
        String description = scanner.nextLine();
        System.out.print("Enter Release Date: ");
        String releaseDate = scanner.nextLine();
        System.out.print("Enter Genre: ");
        String genre = scanner.nextLine();

        int maxUserId = 0;
        for (Movie movie : movies) {
            int userIdInt = Integer.parseInt(movie.getMovieId());
            if (userIdInt > maxUserId) {
                maxUserId = userIdInt;
            }
        }

        // Assign new user ID by incrementing the maximum user ID found
        int newMovieId = maxUserId + 1;

        Movie movie = new Movie(Integer.toString(newMovieId) , title, description, releaseDate, genre);
        movies.add(movie);
        System.out.println("Movie added successfully!");
    }

    private static void postReview() {
        System.out.println("Posting a Review");
        if (currentUser == null) {
            System.out.println("You must be logged in to post a review.");
            return;
        }

//        System.out.println("Posting a Review");
        if (movies.isEmpty()) {
            System.out.println("No movies available to review.");
            return;
        }

        System.out.println("Available Movies:");
        for (int i = 0; i < movies.size(); i++) {
            System.out.println((i + 1) + ". " + movies.get(i).getTitle());
        }

        System.out.print("Select a movie to review (enter number): ");
        int movieIndex = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        if (movieIndex < 1 || movieIndex > movies.size()) {
            System.out.println("Invalid movie selection.");
            return;
        }

        Movie selectedMovie = movies.get(movieIndex - 1);
        System.out.print("Enter Rating (1-5): ");
        int rating = scanner.nextInt();
        scanner.nextLine(); // Consume newline character
        System.out.print("Enter Comments (optional): ");
        String comments = scanner.nextLine();

        int maxUserId = 0;
        for (Review review : reviews) {
            int userIdInt = Integer.parseInt(review.getReviewId());
            if (userIdInt > maxUserId) {
                maxUserId = userIdInt;
            }
        }

        int newReviewId = maxUserId + 1;

        Review review = new Review(Integer.toString(newReviewId),currentUser.getUserId(), selectedMovie.getMovieId(), rating, comments);
        reviews.add(review);

        System.out.println("Review posted successfully!");
    }

    private static void updateReview() {
        if (currentUser == null) {
            System.out.println("You must be logged in to update a review.");
            return;
        }

        System.out.println("Updating Review");

        // Retrieve reviews posted by the current user
        List<Review> userReviews = getUserReviews(currentUser.getUserId());

        if (userReviews.isEmpty()) {
            System.out.println("You haven't posted any reviews yet.");
            return;
        }

        // Display reviews for selection
        System.out.println("Your Reviews:");
        for (int i = 0; i < userReviews.size(); i++) {
            System.out.println((i + 1) + ". " + userReviews.get(i).getMovieId() + " - Rating: " + userReviews.get(i).getRating());
        }

        // Prompt user to select a review for update
        System.out.print("Select a review to update (enter number): ");
        int reviewIndex = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        if (reviewIndex < 1 || reviewIndex > userReviews.size()) {
            System.out.println("Invalid review selection.");
            return;
        }

        // Get the selected review
        Review selectedReview = userReviews.get(reviewIndex - 1);

        // Prompt user for new rating and comments
        System.out.print("Enter new rating (1-5): ");
        int newRating = scanner.nextInt();
        scanner.nextLine(); // Consume newline character
        System.out.print("Enter new comments: ");
        String newComments = scanner.nextLine();

        // Update the selected review
        selectedReview.setRating(newRating);
        selectedReview.setComments(newComments);

        // Update the corresponding document in the database
        MongoCollection<Document> reviewsCollection = database.getCollection("Reviews");
        Document updatedReviewDoc = new Document("rating", newRating)
                .append("comments", newComments);
        reviewsCollection.updateOne(
                Filters.and(
                        Filters.eq("userId", selectedReview.getUserId()),
                        Filters.eq("movieId", selectedReview.getMovieId())
                ),
                new Document("$set", updatedReviewDoc)
        );

        System.out.println("Review updated successfully!");
    }

    private static void removeReview() {
        if (currentUser == null) {
            System.out.println("You must be logged in to remove a review.");
            return;
        }

        // Get reviews associated with the logged-in user
        List<Review> userReviews = getUserReviews(currentUser.getUserId());

        if (userReviews.isEmpty()) {
            System.out.println("You have no reviews to remove.");
            return;
        }

        // Display the user's reviews
        System.out.println("Your Reviews:");
        for (int i = 0; i < userReviews.size(); i++) {
            System.out.println((i + 1) + ". Movie: " + userReviews.get(i).getMovieId() +
                    ", Rating: " + userReviews.get(i).getRating() +
                    ", Comments: " + userReviews.get(i).getComments());
        }

        // Ask the user to select the review to delete
        System.out.print("Enter the number of the review you want to delete: ");
        int reviewIndex = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        if (reviewIndex < 1 || reviewIndex > userReviews.size()) {
            System.out.println("Invalid review selection.");
            return;
        }

        // Get the selected review
        Review selectedReview = userReviews.get(reviewIndex - 1);

//        // for testing
        String validation_check = selectedReview.toString();
        System.out.println(validation_check);

        // Remove the selected review from the in-memory list
        reviews.remove(selectedReview);

        // Remove the selected review from the database
        MongoCollection<Document> reviewsCollection = database.getCollection("Reviews");
        reviewsCollection.deleteOne(
                Filters.and(
                        Filters.eq("userId", selectedReview.getUserId()),
                        Filters.eq("movieId", selectedReview.getMovieId())
                )
        );

        System.out.println("Review removed successfully.");
    }

    private static List<Review> getUserReviews(String userId) {
        List<Review> userReviews = new ArrayList<>();
        for (Review review : reviews) {
            if (review.getUserId().equals(userId)) {
                userReviews.add(review);
            }
        }
        return userReviews;
    }

    private static void listMoviesWithReviews() {
        System.out.println("Movies with Reviews:");
        for (Movie movie : movies) {
            System.out.println(movie.getTitle() + " - " + movie.getDescription());
            System.out.println("Reviews:");
            boolean hasReviews = false;
            for (Review review : reviews) {
                if (review.getMovieId().equals(movie.getMovieId())) {
                    // Fetch user for the review
                    User user = findUserById(review.getUserId());
                    if (user != null) {
                        hasReviews = true;
                        System.out.println(user.getUsername() + ": \nRating: " + review.getRating() + ", Comments: " + review.getComments());
                    }
                }
            }
            if (!hasReviews) {
                System.out.println("No reviews yet.");
            }
            System.out.println();
        }
    }


    public static void getAllUserDetails() {
        System.out.println("All User Details:");
        for (User user : users) {
            System.out.println("User UUID: " + user.getUserId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Email: " + user.getEmail());
            System.out.println();
        }
    }

    // Method to retrieve details of all movies
    public static void getAllMovieDetails() {
        System.out.println("All Movie Details:");
        for (Movie movie : movies) {
            System.out.println("Movie UUID: " + movie.getMovieId());
            System.out.println("Title: " + movie.getTitle());
            System.out.println("Description: " + movie.getDescription());
            System.out.println("Release Date: " + movie.getReleaseDate());
            System.out.println("Genre: " + movie.getGenre());
            System.out.println();
        }
    }

    // Method to retrieve details of all reviews
    public static void getAllReviewDetails() {
        System.out.println("All Review Details:");
        for (Review review : reviews) {
            System.out.println("User UUID: " + review.getUserId());
            System.out.println("Movie UUID: " + review.getMovieId());
            System.out.println("Rating: " + review.getRating());
            System.out.println("Comments: " + review.getComments());
            System.out.println();
        }
    }

    // Method to add some test data (for testing purposes) {until we get the stupid database up}
    private static void addTestData() {
        movies.add(new Movie("1", "The Shawshank Redemption", "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.", "1994", "Drama"));
        movies.add(new Movie("2", "The Godfather", "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.", "1972", "Crime"));
        movies.add(new Movie("3", "The Dark Knight", "When the menace known as The Joker emerges from his mysterious past, he wreaks havoc and chaos on the people of Gotham.", "2008", "Action"));

        // Assuming user and movie IDs are known
//        reviews.add(new Review("1", "movie_uuid_1", 5, "A masterpiece!"));
    }

}
