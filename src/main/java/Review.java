public class Review {
    private String reviewId;
    private String userId;
    private String movieId;
    private int rating;
    private String comments;

    // Constructor
    public Review(String reviewId,String userId, String movieId, int rating, String comments) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
        this.comments = comments;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "Review{" +
                "userId='" + userId + '\'' +
                ", movieId='" + movieId + '\'' +
                ", rating=" + rating +
                ", comments='" + comments + '\'' +
                '}';
    }
}
