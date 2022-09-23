The android app aims is for a French users to get pokemon cards translated in French.

The app requires the user to take a picture. The picture is then analysed (with ML Kit) to determine the card number and the total family card number.
If only one family has this this total, the process stops. If not, a TensorFlow model is used to read picture chuncks to determine the picture family.
The browser is open with a page that will open the pokemon card in French.

Limitations:
This app is currently knowing 33 families. The TensorFlow model knows only 19 families.
Japonese cards are not managed.
