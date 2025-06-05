FROM node:20-alpine

WORKDIR /app

# Copy package.json and package-lock.json (if available)
# Using package-lock.json is recommended for reproducible installs
COPY package*.json ./

# Install dependencies
# Use npm ci for cleaner, faster, and more reliable builds in CI/CD environments
# It requires a package-lock.json or npm-shrinkwrap.json
# If you don't have a lock file, use npm install
RUN npm ci --only=production
# If you also need devDependencies for some reason (e.g. running tests in image, though not typical for slim production image):
# RUN npm ci
# Or if no lock file: 
# RUN npm install --only=production

# Copy the rest of the application code
COPY . .

# Application port as specified in docker-compose and app.js
EXPOSE 8083

# Command to run the application
# Ensure app.js is the correct entry point
CMD ["node", "app.js"] 