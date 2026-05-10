//specific type of runtime exception that involves our type of syntax errors
class SyntaxErrorException extends RuntimeException {
    public SyntaxErrorException(String message) {
        super(message);
    }
}