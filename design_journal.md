# Catwalk Design Journal

Mini-todo cos wrong branch

* implement model as a tree, not a list

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

Then your model would be defined like this

```clojure
;; period-number
(increment (previous period-number))

;; market-value
(product (profile mv-growth)
         (previous value-carried-forward))

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