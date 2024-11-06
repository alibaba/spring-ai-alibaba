# Use the official PostgreSQL image as the base
FROM postgres:16

# Set environment variables for PostgreSQL
ENV POSTGRES_USER=age
ENV POSTGRES_PASSWORD=AgeVector

# Install necessary dependencies and extensions
RUN apt-get update && \
    apt-get install -y \
    build-essential \
    libreadline-dev \
    zlib1g-dev \
    bison \
    flex \
    git \
    curl \
    postgresql-server-dev-16
 #   nodejs \
 #   npm

# Clone Apache AGE
RUN git clone https://github.com/apache/age.git /age

# Build and install Apache AGE
RUN cd /age && \
    make && \
    make install

# Clone pgvector extension
RUN git clone https://github.com/pgvector/pgvector.git /pgvector

# Build and install pgvector
RUN cd /pgvector && \
    make && \
    make install

#ENV NODE_OPTIONS="--openssl-legacy-provider"

# RUN git clone https://github.com/apache/age-viewer.git && \
#     cd age-viewer && \
#     npm install pm2 && \
#     npm run setup

# Add Apache AGE and pgvector to the shared_preload_libraries
RUN echo "shared_preload_libraries = 'age,vector'" >> /usr/share/postgresql/postgresql.conf.sample

# Expose PostgreSQL port
#EXPOSE 5432

#RUN useradd --user-group --system --create-home --no-log-init app

# Start PostgreSQL



#CMD ["/cmd_command.sh"]
CMD ["postgres"]