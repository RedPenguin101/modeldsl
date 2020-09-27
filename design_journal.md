# Catwalk Design Journal

## Ubiquitous Language

* Model
* Profile
* Scenario 
* ModelRow: A named concept in your model, corresponding to a row of the scenario.
* ModelRowDefinition: Declarative statement of how a ScenarioValue can be calculated from other ScenarioValues and Profile Attributes 
* Period: A period of time, representing for example a month or quarter or year.
* ScenarioValue: The calculated numeric value of a ModelRow for a given Period
* Entity: The thing being modelled - a house's value, some shares, an investment.
* Analytics: Sits on top of a Scenario and provides summary information and statistics. Comes packaged with a Model.
* Instance: a persisted profile and model.
* Case variants: Within an Instance there can be multiple cases. The main one is the Base, but new cases can be created by tweaking the profile and definitions. 
* ModelTemplate: a list of ModelRows that can be reused. Can be provided with or without default ModelRowDefinitions. For example a European Real Estate ModelTemplate. The user can override the default ModelRowDefinitions, add additional intermediate ModelRows, but for a particular Model, all ModelRows in the Template being used must be implemented.

## 26th September

Haven't picked this one up in a while. Here we go.

First a bit about Catwalk.

In my industry (asset management) we do a lot of financial modelling. Modelling in this context meaning that, based on a set of historical facts, and a set of assumptions about the future, we want to create a time series of cash flows or other financial information that we think are likely to happen. Usually we are modelling some sort of investment, but really it could be anything that has a financial element.

To take an extremely simple example, let's say we want to model the cash flows from some public equity investment we hold. We might have as a starting fact the current market value of the shares, and the current price/earnings (PE) ratio. We might decide to use the following assumptions:

1. that the share price will increase by 2% a year
2. that the PE ratio will stay constant
3. that dividends will be 40% of earnings annually.
4. that we will sell the shares at their market value after 4 years of collecting dividend income.

From this we can create a time series of the future cash flows we can expect to get from these shares.

Often, models will be long lived - that is, after year one, the actual figures that ocurred will be plugged in in place of the projected numbers, assumptions updated, and the model re-run for future periods using the new assumptions and historical information.

The investment analyst will usually create an Excel model for this. These models can get extremely large and difficult to understand. The complexity is partly due to the inherent complexity of the problem, but mostly due to the fact that Excel has no easy means of abstraction or composition. You're usually going to be dealing with the lowest level stuff there is, and dealing with very nested or complicated formula. Probably I'll talk more about that later, but for now let's just say the Excel models can get pretty nightmarish.

The goal of Catwalk is to provide an application that is more tailored to creating these sorts of models, with the biggest thing being the provision of a domain specific language (DSL) for writing these models.

The definitions for the above model might look something like this:

First you have a profile that contains historical facts and assumptions

```clojure
{:starting-mv 100
 :mv-growth 0.02
 :pe-ratio 0.1
 :dividend 0.4
 :sell-period 4}
```

Then the 'rows' of your model would be defined like this

```clojure
;; period-number
(increment (previous period-number))

;; market-value
(product (profile mv-growth)
         (previous value-carried-forward 
                   {:initial (lookup starting-mv)}))

;; earnings
(product (this market-value)
         (profile pe-ratio))

;; dividend
(product (this earnings)
         (profile dividend))

;; sale-proceeds
(if (equal (this period-number)
           (profile sell-period))
  (this market-value)
  0)

;; value-carried-forward
(sum (this market-value)
     (negative (this sale-proceeds)))

;; cumulative-cash
(sum (aggregate sale-proceeds)
     (aggregate dividend))
```

And result of applying the model to the profile would be:

| period-number         | 1   | 2    | 3      | 4      | 5 |
|-----------------------|-----|------|--------|--------|---|
| market-value          | 100 | 102  | 104.04 | 106.12 | 0 |
| earnings              | 10  | 10.2 | 10.4   | 10.61  | 0 |
| dividend              | 4   | 4.08 | 4.16   | 4.24   | 0 |
| sale-proceeds         | 0   | 0    | 0      | 106.12 | 0 |
| value-carried-forward | 100 | 102  | 104.04 | 0      | 0 |
| cumulative-cash       | 4   | 8.08 | 12.24  | 112.6  | 112.6 |


The benefits of this over doing this in Excel is firstly that you are defining your model explicitly with reference to other model elements. Typically in Excel your model would be defined as Excel formula. For example a measure for period 4 might be `AA4*Control!C45`. Clearly it's impossible to tell what this actually represents. Is it right? You can't tell without looking up the logic behind it. 

Notice as well that with Excel you only ever have the _application_ of the model, and the abstract model itself is never defined. So if the measure for period 4 is `AA4*Control!C45`, then the measure for period 5 might be `AB4*Control!C45`. From this you might infer that the abstract definition model for this measure is 'the previous periods value times something in the control tab'. Contrast that to the DSL, where your model is always explicit: `(product (this earnings) (profile dividend))`. It's much more comprehensible without digging through formula.

## The fundamental model of modelling (27th Sept)

Our construct as three top level ideas:

1. The Profile
2. The Model
3. The Scenario

### The Profile

The profile contains information you want to look up. It might include factual information (such as the market value of shares at a point in time) or assumptions (such as an assumption that the growth rate in the share price will be 2% per year)

### The Model

The model consists of the names of the 'rows' of the model, and for each name, the definition of how a measure in that row will be calculated with reference to other rows in this or previous periods. For example, the definition of the row `period-number` might be `(increment (previous period-number))`. The definition can also include reference to information in the profile.

### The Scenario

The Scenario is the result of 'running' the model. You pass the Profile and Model into some 'run' function, and the output will be a time series of periods containing each name from the model, together with a value for each name/period combination.

## Usage (27th Sept)

The user will want to represent 'the thing being modelled' - that is the cash flows from shares, a Fund, the value of a house. This will have to be first class, since the user will want to say 'I want to load up the XYZ Deal and look at the latest metrics about it'. Call this the *Entity*.

Models are independent of Entities, though there is the expectation that if a ModelDefinition includes a reference to a profile attribute, when the Model is used to generate a Scenario it must be with a Profile that has that attribute.

A typical user pattern might look like this:

### Initial Creation and Purchase of Investment
1. An investment firm is thinking about buying a Spanish real estate development they refer to as Bravo.
2. The user, an Investment Analyst, loads up Catwalk
3. They create the Bravo entity
4. They bring in a ModelTemplate from the Library called 'European Real Estate' (ERE). This contains various Model Rows with default Definitions which are relevant to modelling real estate. They may or may not tweak the ModelDefinitions for relevance to this particular Entity.
5. They create a profile, which will include various relevant information, including everything needed by the ERE Model.
6. This is all done in the editing view, which has the Profile, Model, Scenario and Analytics displayed at all times. Any changes the analyst makes to the Model or Profile regenerates the scenario and analytics so the analyst can see the effects the changes have on the Scenario.
7. When the analyst is happy, they save the Profile and Model as the 'Primary_2020-09-28' Instance.
8. From The Primary they can create multiple Cases (in addition to the main one of the Instance, known as the Base Case), where they specify tweaks to the profile, model definitions or overwrites for particular ScenarioValues. They might create an Upside and Downside case in addition to the Base
9. They can use the analytics from the various cases of the Instance, or just export the difference Scenarios, to create proposal which is taken to an Investment Committee. For arguments sake, lets say this happens, the investment is approved, and the RE development is purchased on 2020-09-30.

### 3 Months in
1. 3 Months after the investment is made (after December 2020, say), the first reporting package from Bravo is received, containing historical time series data. Ideally it will be in a format that matches the Model being used by the investment firm.
2. If not it will have to be conformed, but either way the historical periods get made available to Catwalk through an API, wth a common ID for the Entity and Model so Catwalk knows that it relates to the Bravo Entity using the European Real Estate Model.
3. The Analyst loads up Catwalk and navigates to the Primary Instance of the Bravo Entity.
4. She sets the ScenarioDate to December 2020. The Instance is now pulling in the historical data from the above steps and using that to create the ScenarioValues for the first 3 periods, then using the Model and Profile to create all subsequent ones.
5. She sets the ComparisonDate to 3 months prior (Underwrite), and is able to see the changes in the Scenario at underwrite and now that historical data has been collected.
6. After some thinking and analysis, she decides that she needs to change some things about the model to best reflect her estimate of what the future will look like going forward.
7. She Versions the Instance (creating 'Primary_2021-01-28'), and modifies the Profile and ModelDefinitions to reflect her new theories about the future. She can also tweak the Case variants if needed. The Model itself though, in terms of what ModelRows it contains, must remain the same.
8. At this point the Analyst can compare a lot of things about the Scenario/Analytics:
  a. what actually happened in the first period compared to what we thought would happen in the first period as-at underwrite
  b. what actually happened in the first period compared to our revised Instance says would happen in the first period (which one would expect to show less of a difference)
  c. what we expected to happen in future periods under the conditions of our initial Instance before we had any historical information vs. what we expect to happen under the conditions of our initial Instance given the historical information we have
  d. given the historical information, what we expect to happen in future periods under the initial Instance vs. what we expect to happen under the revised Instance.

### 12 Months in
1. We've now been getting historical data for 12 months.
2. The analyst decides the Model we're using is getting very creaky and can no longer be effectively used to predict what will happen to the investment in the future.
3. She decides to abandon the old Instance and start with a new ModelTemplate call 'New and Better Real Estate' that she and her colleague have been developing.
4. She creates a new Instance in the Bravo Entity called 'Improved_2021-10-14', selecting the new Model template.
5. She has told the RE developer to regenerate the historical information to include the time series information using the new ModelRows, and provide it in this format going forward, and it's now available to Catwalk
6. Using this she re-models the Entity using the new Model from day one, using the re-formulated historical information as a benchmark of the Model's accuracy.
7. This Instance is now used as the default one for the Entity going forward.

## Data Model (27th Sept)

```clojure
;; Entity
{:id :xyz
 :name "Bravo"
 :description "A Spanish real estate development"
 :instances [:Instance]}

;; Instance
{:id :def
 :name "Primary"
 :creation-date "2020-09-29"
 :profile :Profile
 :model :Model
 :cases [:Case]
 :analytics [:Analytics]
 :succeeds :abc}

;; Historical Period
{:entity :xyz
 :instance :abc
 :periods [{:period-number 1
            :rent 100
            :expenses -50}
           {:period-number 2
            :rent 120
            :expenses -60}]}
```

## Scaling, Abstraction and Sub-models (27th Sept)

A simple financial model may consist of only a few rows. In practice, however, they can grow very large - hundreds of rows spread across dozens of tabs. If we define our model as we have so far, it will certainly not be very scalable, since it will be incomprehensible for anything more that around 20 Model Rows.

What we need is a means of abstraction. A way to encapsulate parts of the model in such a way that it can be used as a single thing higher in the model hierarchy.

For example, if we are modelling the free cash flow of a Brazilian mining operation, we might want to model Revenue from the mine, and say that is defined as 

```clojure
(product (this ore-price) (this ore-produced))
```

`ore-price` we might have decided to model fairly simply based on some assumption about global economic growth rates.

But we may decide that `ore-produced` requires a much more complicated derivation, that the generation of this number is in fact the output of a completely separate model of the operations of the mine. If we were to include all the pieces necessary to properly derive this in our free-cash-flow model (call it _A_), then we would have so many rows that the whole thing would be incomprehensible.

What we want to do is create a separate submodel, _B_, representing the mines operations, the output of which is the `ore-produced`

```clojure
;; defn of ore-produced in Free Cash Flow model A
(sub-model Operations)
```

So we need the ability to define a sub-model, which will have an output ScenarioValue, which can be referenced by other sub-models (everything except the output row in the sub-model will be private and inaccessible to other models.)