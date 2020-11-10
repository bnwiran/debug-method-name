# debug-method-name

Description
------------
A tool of spotting and refactoring inconsistent method names learned from real-world code bases.
This work will be presented at ICSE 2019.

Requirement
------------
  - Java 1.8
  - Maven 3.3.9

How to run debug-method-name
-----------------------------
1. Clone the PatchParser:
  - `git clone https://github.com/SerVal-DTF/debug-method-name.git`
  
2. Prepare data: 
   **Note that:** it will take **a long time and a big space** to prepare the data for this experiment, we recommend to use the data we already have to proceed the following steps.
  - `./data_prepare.sh'
  
3. Model Learning:
   **Note that:** it will take **a long time and a huge space** to finish the learning process, we recommend to use our learned model to proceed the following step.
  - `./model_learn.sh`

4. Spot and Refactor inconsistent method names:
  - `./spot_refactor.sh`
