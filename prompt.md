This project is about implementing a multiplayer storytelling game.

# Main Idea

A different version of a story is sent to each player of the game. After a certain amount of time of reading the story,
the game starts. An empty text block is shown to each player. A random player is chosen to be first, after which players
take turns. In each turn, the player appends a single word or punctuation at the end of the text block. The player must
submit their word or punctuation under a limited amount of time. The word or punctuation the player inserted is
synchronized to all players. Each player has a timer that counts down in their turn. Players whose timers run out lose
their turns afterward. The game ends when the timers of all players run out. After the game ends, a score is calculated
for each player based on how close the story presented in the text block is to the original story shown to them. A
certain amount of score is deducted for any words or punctuation inserted that cause grammatical, semantical or logical
errors. The player with the highest score wins.

# Implementation Details

## Infrastructure

The software is separated into two parts: the frontend and the backend. The frontend is a web application written in
HTML and JavaScript. It includes the basic UI and handles user interactions, such as displaying the story and text edit
block. The backend is a server program written in Java. It connects to a remote large language model through a provided
URL and API key and opens to a certain port that is outputted.

### Frontend

Created a directory in the src folder for storing HTML and JavaScript files for the frontend.

#### UI

The first page of the UI includes two text fields and a button. The first text field is for entering the server address.
The second field is for entering the player's name. The name should consist of only ASCII characters without whitespace
characters. The button is used to connect to the server. After connecting to the server, the player is redirected to the
game page.

The game page is divided into several sections. In the middle of the screen, there is a large text block that would be
used for displaying the story and editing. At the top of the screen, an area for one line of text is left for displaying
instructions. On the left edge of the screen, room for a sidebar should be left for displaying the player list. For each
player in the list, the player's name should be displayed, as well as their time left, displayed under the name. The
sidebar should be collapsible. In the top right corner of the screen, on the same line as the instructions, the timer
for this player is displayed. A long button is situated along the bottom edge of the screen, displaying the text
"Submit". The button should only be clickable in this player's turn and after a valid sequence of letters or punctuation
is entered. At the end of the game, the player is redirected to the score page.

Except for the time when it's this player's turn, the text block should be unmodifiable. In this player's turn, the
player should only be able to append a single word or punctuation at the end of the text block. They should not be
allowed to modify the existing text or insert whitespace characters.

The score page only displays a list of players, their scores and ranking.

The UI style should be modern, using a flat design. **The details of the UI can be modified as long as it can provide a
better user experience.**

#### Communication with the Backend

On the first page, when the button is clicked, give an alert if the server address is invalid or not found, and send the
player's name to the backend server otherwise. The frontend should receive a response containing the story and the time
to read the story, which are used in the game page.

On the game page, several data should be synchronized among all players, including but not limited to the text in the
text block, all the players' names, timers, as well as the time left for the current turn.

When the game ends, the frontend should receive a response containing the final score of each player so that they can be
displayed on the score page.

### Backend

Create a directory inside the src folder for storing all the Java files for the backend.

#### LLM Invocation

When the program is run, a hint for input should be printed, asking for the URL and API key for the remote large
language model. The URL and API key are then verified, and the program should connect to the model.

#### Game Process

After the remote LLM is connected, a server is started for receiving request from the frontend. After that, a hint
should be printed, asking for the theme of the story. A prompt describing the theme should be sent to the remote LLM,
asking for it to generate a different version of the story for each player and also determining the total time for each
player. The time for each turn can be pre-determined (30 seconds recommended). The different stories should be similar
in the characters involved and settings, but with different or even opposite plot development.

The different versions of the story are then sent to each player one by one. Then players take turns editing the text
block in the process stated in "Main Idea".

After the game ends, send the text in the text block to the remote LLM and ask for it to give a score for each of the
players one by one based on how close the text is to the corresponding story given to each player. Meanwhile, give the
remote LLM all words and punctuations inserted by each player one by one, asking it for a deduction on the player's
score if any grammatical, semantical or logical errors are made.

The scores are then sorted and sent to the frontend to be displayed.

A prompt format for each LLM request mentioned above should be written in a .md file, stored in the "prompts" folder
under the backend directory. These should include the story generating prompt, the score prompt and the score deduction
prompt.

**The structure of the backend server program and the protocol of communication between the frontend and the backend can
be modified as long as it can provide better efficiency for the software.**