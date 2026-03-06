-- Minimal Chinook-like schema for SQL agent demo (H2)
CREATE TABLE IF NOT EXISTS Genre (
    GenreId INT PRIMARY KEY,
    Name VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS Track (
    TrackId INT PRIMARY KEY,
    Name VARCHAR(200) NOT NULL,
    AlbumId INT,
    MediaTypeId INT NOT NULL,
    GenreId INT,
    Composer VARCHAR(220),
    Milliseconds INT NOT NULL,
    Bytes INT,
    UnitPrice DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (GenreId) REFERENCES Genre(GenreId)
);

CREATE TABLE IF NOT EXISTS Album (
    AlbumId INT PRIMARY KEY,
    Title VARCHAR(160),
    ArtistId INT
);

CREATE TABLE IF NOT EXISTS Artist (
    ArtistId INT PRIMARY KEY,
    Name VARCHAR(120)
);

INSERT INTO Genre (GenreId, Name) VALUES (1, 'Rock'), (2, 'Jazz'), (3, 'Metal'), (18, 'Sci Fi & Fantasy'), (19, 'Science Fiction'), (20, 'Drama'), (21, 'TV Shows'), (22, 'Comedy');
INSERT INTO Artist (ArtistId, Name) VALUES (1, 'AC/DC'), (2, 'Accept'), (3, 'Aerosmith');
INSERT INTO Album (AlbumId, Title, ArtistId) VALUES (1, 'For Those About To Rock', 1), (2, 'Balls to the Wall', 2), (3, 'Fast As a Shark', 3);
INSERT INTO Track (TrackId, Name, AlbumId, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice) VALUES
(1, 'For Those About To Rock', 1, 1, 1, 'Angus Young', 343719, 11170334, 0.99),
(2, 'Balls to the Wall', 2, 2, 1, 'U. Dirkschneider', 342562, 5510424, 0.99),
(3, 'Fast As a Shark', 3, 2, 1, 'F. Baltes', 230619, 3990994, 0.99),
(4, 'Sci Fi Theme', 1, 1, 18, 'Unknown', 5000000, 0, 0.99),
(5, 'Drama Theme', 1, 1, 20, 'Unknown', 4000000, 0, 0.99);
