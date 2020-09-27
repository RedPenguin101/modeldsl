# TODO

* DONE fix updating button behaviour in model-display component
* DONE change to Bulma css
* DONE reimplement model component with dropdown
* DONE edn formatting for textareas
* DONE user inputs without : - gets parsed to keywords
* DONE decimal formatting for output window
* DONE when a row in the output window is clicked on, change the current row selection to that
* DONE output has string headers
* DONE zeros format to "-"
* DONE disable text wrapping of row names
* DONE Add a new model row
* DONE Delete model row
* DONE implement re-ordering
* DONE change tabulate to take row-order from app-db
* DONE dropdown should be fixed width
* DONE delete buttons should be far right
* DONE fix codemirror it actually works
* DONE make sure 'initial value' is working
* DONE implement equal function
* DONE implement negate function

* Top bar
* Implement Entity and persistence
  * Top bar where you can select entity from a dropdown
  * Change App state to have one Entity at a time
  * save-entity event which dumps the app-state to disk (maybe fired on update?)
  * load-entity event which loads the entity from disk
  * have available entities in app-state (name->id pairs)
  * lookup available entities on startup, maybe from a separate edn file
* output is more descriptive about validity
  * recognizes undefined functions
  * recognizes circular dependency
* doesn't fail on a vector being returned from profile-lookup
* date capability save-entity events dumping edn to drive.
* Implement Instance
  * Another thing on the top bar
* change number of periods to model
* indicate unsaved profile / model
* export model to xlsx
* Export scenario to xlsx
* Historical data interface
* implement model as a tree, not a list
* Implement sub model