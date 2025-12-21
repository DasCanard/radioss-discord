# 📻 Radioss Discord Bot

A Discord bot for radio stations that uses the Radio Browser API and provides audio streaming directly to Discord voice channels. The bot is operated via slash commands and offers an intuitive search for radio stations.

**Hosted by me:** (24/7 HQ): [Invite](https://discord.com/oauth2/authorize?client_id=1376904142412316812)👋

## ✨ Features

- 🔍 **Search for Radio Stations** by name, country, or genre
- 🏆 **Top Radio Stations** sorted by popularity
- 🌍 **Country-specific Search** for regional stations
- 🎵 **Genre-based Search** (Rock, Pop, Jazz, News, etc.)
- 🎲 **Random Radio Stations** for discovery
- 🎧 **Audio Streaming** directly to Discord voice channels
- 💎 **Interactive Buttons** for easy operation
- 🎯 **Intuitive Slash Commands**
- 🔊 **Volume Control** with dropdown menu
- 📱 **Pagination** for search results
- ⭐ **Favorites System** for personal station lists
- 🔄 **Reconnection Logic** on crash or update
- 🕐 **24/7 Mode** for continuous operation

## 🚀 Setup

### 1. Create Discord Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new Application
3. Go to "Bot" and create a bot
4. Copy the bot token
5. Go to "OAuth2" → "URL Generator"
   - Scopes: `bot`, `applications.commands`
   - Bot Permissions: `Send Messages`, `Use Slash Commands`, `Embed Links`, `Connect`, `Speak`
6. Invite the bot to your server with the generated URL

### 2. Start Bot

#### Option 1: Docker (Recommended)
```bash
# Create environment file
echo "DISCORD_BOT_TOKEN=your_bot_token_here" > .env

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f radioss-bot
```

#### Option 2: With Environment Variable
```bash
export DISCORD_BOT_TOKEN="your_bot_token_here"
./gradlew run
```

#### Option 3: Build and Run JAR
```bash
./gradlew shadowJar
java -jar build/libs/radioss-discord-0.1.0-all.jar
```

## 🎮 Commands

Use `/help` in Discord to view all available commands and their usage.

## 🐳 Docker Deployment

### Development
```bash
# Build and run locally
docker-compose up --build

# Stop the bot
docker-compose down
```

### Production
```bash
# Update to latest version
docker-compose pull
docker-compose up -d
```

### Environment Variables
Create a `.env` file:
```env
DISCORD_BOT_TOKEN=your_discord_bot_token_here
```

## 🛠️ Technical Details

- **Language:** Kotlin
- **Discord Library:** JDA 5.1.0
- **Audio Library:** LavaPlayer 1.3.78
- **HTTP Client:** OkHttp 4.12.0
- **API:** [Radio Browser API](https://www.radio-browser.info/) ([Selfhosted](https://stations.radioss.app/))
- **Build Tool:** Gradle
- **Java Version:** 21


## 🔧 Development

### Install Dependencies
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Build JAR with all Dependencies
```bash
./gradlew shadowJar
```

### Docker Development
```bash
# Build image locally
docker build -t radioss-discord .

# Run with local image
docker run -e DISCORD_BOT_TOKEN=your_token radioss-discord
```

## 🌟 Upcoming Features

Planned enhancements:
- ⭐ **Favorites System** for personal station lists
- 🔍 **Station Search** directly in `/play` command
- 📊 **Advanced Statistics** about radio stations

## 📄 License

This project is released under the MIT License.

## 🙏 Credits

- [Radio Browser API](https://www.radio-browser.info/) for the comprehensive radio database
- [JDA](https://github.com/DV8FromTheWorld/JDA) for Discord integration
- [LavaPlayer](https://github.com/sedmelluq/lavaplayer) for audio streaming
- [OkHttp](https://square.github.io/okhttp/) for HTTP requests 
