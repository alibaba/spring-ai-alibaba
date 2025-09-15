# Generate self-signed certificate for testing purposes
# This command creates a new RSA private key and a self-signed certificate valid for 365
openssl req -x509 -newkey rsa:4096 -keyout src/test/resources/ssl/key.pem -out src/test/resources/ssl/cert.pem -days 365 -nodes -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,DNS:127.0.0.1,IP:127.0.0.1"
# Convert the PEM files to a PKCS12 keystore if need jks config
openssl pkcs12 -export -in src/test/resources/ssl/cert.pem -inkey src/test/resources/ssl/key.pem -out src/test/resources/ssl/keystore.p12
