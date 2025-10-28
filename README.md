# pdfchecker

# Build
mvn clean package

# Check single PDF
java -jar target/pdf-checker-1.0.0.jar /path/to/your/problematic.pdf

# Check multiple PDFs
java -jar target/pdf-checker-1.0.0.jar file1.pdf file2.pdf file3.pdf

# Check all PDFs in a directory (Linux/Mac)
java -jar target/pdf-checker-1.0.0.jar /path/to/pdfs/*.pdf

