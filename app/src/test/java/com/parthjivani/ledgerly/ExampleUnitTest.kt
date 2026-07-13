package com.parthjivani.ledgerly

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testUserCalculations() {
    val block1 = """
2025-09-23,90.0,Dinner
2025-09-23,370.0,Fuel
2025-09-25,130.0,Dinner
2025-09-28,532.0,Dinner
2025-09-29,90.0,Dinner
2025-10-01,143.0,Ice cream
2025-10-02,90.0,Dinner
2025-10-04,110.0,Dinner
2025-10-05,209.0,Dinner
2025-10-05,131.26,Dinner
2025-10-06,125.0,Dinner
2025-10-07,90.0,Dinner
2025-10-07,370.0,Fuel
2025-10-08,110.0,Dinner
2025-10-08,370.0,Fuel
2025-10-09,152.0,Ice cream
2025-10-09,130.0,Dinner
2025-10-10,160.0,Dinner
2025-10-11,110.0,Dinner
2025-10-11,80.0,Breakfast
2025-10-28,370.0,Fuel
2025-10-29,344.0,Lunch
2025-10-29,90.0,Dinner
2025-10-31,90.0,Dinner
2025-11-04,400.0,Fuel
2025-11-05,794.0,D Mart
2025-11-10,428.19,Health
2025-11-10,400.0,Fuel
2025-11-12,70.0,Dinner
2025-11-13,50.0,Health
2025-11-13,228.19,Health
2025-11-16,70.0,Dinner
2025-11-19,425.9,Pizza
2025-11-19,370.0,Fuel
2025-11-20,130.0,Dinner
2025-11-24,140.0,Dinner
2025-12-08,2999.0,Decathelon Jacket
2025-12-09,90.0,Dinner
2025-12-10,3799.0,Lenskart
2025-12-11,423.63,Fuel
2025-12-13,70.0,Khichadi
2025-12-15,373.99,McDonald's 
2026-04-25,1100.0,Fuel
2026-05-10,1299.0,Campus sliders
2026-05-17,1227.36,Fuel
    """.trimIndent()

    val block2 = """
2025-01-01,10,
2025-01-01,300,
2025-01-01,90,Groceries
2025-01-02,1250,9196 Accessories
2025-01-02,4490,Samsung A23 Screen
2025-01-03,150,Puncture
2025-01-03,100,
2025-01-03,150,
2025-01-03,135,
2025-01-03,250,Mobile Glass & Cover
2025-01-03,100,
2025-01-03,35,Cheese
2025-01-04,40,
2025-01-04,5,
2025-01-04,150,Sev Usal
2025-01-07,12400,Groundnut oil
2025-01-07,100,Bus ticket
2025-01-08,3500,Home wiring
2025-01-08,450,Home Key
2025-01-08,285,Pizza
2025-01-10,5,
2025-01-11,300,All Out
2025-01-11,10,
2025-01-11,100,Udhar return to RJ
2025-01-11,70,Perfume
2025-01-13,1500,
2025-01-13,420,Groceries
2025-01-14,10000,
2025-01-15,110,
2025-01-15,800,Home
2025-01-15,15,
2025-01-15,500,Innova
2025-01-16,100,Kadhi Khichadi
2025-01-16,320,Gujarati full dish
2025-01-16,390,Mathho
2025-01-16,80,Milk and snacks
2025-01-16,100,Jio AirFiber Bill
2025-01-16,5,Charges
2025-01-18,35,Milk
2025-01-18,15,Buttermilk
2025-01-18,20,Buttermilk
2025-01-18,285,
2025-01-18,135,Chevda no saman
2025-01-18,110,Ice cream
2025-01-19,55,Ice cream
2025-01-20,80,"Cheese, Oragano"
2025-01-20,40,Sprite
2025-01-21,130,Cheese
2025-01-21,120,
2025-01-22,20,Buttermilk
2025-01-23,200,Airtel Bill 9712338989
2025-01-24,130,Xerox
2025-01-24,200,Airtel Bill 9913071189
2025-01-24,380,
2025-01-24,14000,Printer
2025-01-24,5,Xerox
2025-01-24,25,Xerox
2025-01-24,195,"Sugar, Milk, Buttermilk"
2025-01-24,1000,Jumper bullet
2025-01-24,10,Gems
2025-01-24,20,Buttermilk
2025-01-24,250,
2025-01-25,40,Buttermilk
2025-01-25,55,"Milk, Farali chevdo"
2025-01-25,240,Printing page
2025-01-25,20,
2025-01-25,20,"Pan, Xerox"
2025-01-26,40,Switch board plug
2025-01-26,40,Buttermilk
2025-01-26,5,
2025-01-27,230,Dr. FIXIT Waterproofing
2025-01-27,20,Xerox
2025-01-27,20,Collector office
2025-01-28,800,Society maintenance
2025-01-28,10,Pan
2025-01-28,40,"Vada pav, Bread pakoda"
2025-01-28,30,Fruit
2025-01-28,530,Activa oil
2025-01-28,285,
2025-01-28,60,Buttermilk
2025-01-29,1200,Sofa repairing
2025-01-29,200,Bib cock
2025-01-29,300,Airtel Bill 9426890029
2025-01-29,70,Milk
2025-01-29,115,Ice cream
2025-01-29,40,Airtel Bill 9712579779
2025-01-30,50,Toilet jet
2025-01-31,170,Hair cut
2025-01-31,20,Buttermilk
2025-01-31,260,Airtel Bill 9712579779
2025-01-31,430,Ishan
2025-01-31,50,Baloon
2025-01-31,25,
2025-02-01,10,Water bottle
2025-02-02,210,Ice cream
2025-02-02,20,Pan
2025-02-02,2000,
2025-02-02,500,
2025-02-03,310,Chhole kulche
2025-02-03,100,
2025-02-03,320,Chinese
2025-02-03,60,Pepsi
2025-02-03,500,
2025-02-03,635,
2025-02-04,25000,HK
2025-02-04,15,BRTS
2025-02-04,120,GSRTC
2025-02-05,100,Oranges
2025-02-05,100,Red velvet mattho
2025-02-05,35,Medicine
2025-02-07,65,Snacks
2025-02-07,25,
2025-02-08,5,
2025-02-09,75,"Milk, buttermilk"
2025-02-09,200,Samosa
2025-02-10,50,Farali chevdo
2025-02-10,305,
2025-02-10,90,"Milk, buttermilk"
2025-02-11,70,Milk
2025-02-11,70,Snacks
2025-02-12,70,Astar for SSJ
2025-02-14,250,
2025-02-14,20,Pani puri
2025-02-14,20,Buttermilk
2025-02-14,20,"Hair colour, cheese"
2025-02-14,160,Sandwich
2025-02-14,45,"Hair colour, cheese"
2025-02-15,1300,Fridge repairing
2025-02-15,250,BOB
2025-02-15,300,Snacks
2025-02-15,50,Vada pav
2025-02-16,60,Jaggery
2025-02-16,710,Jio AirFiber Bill
2025-02-16,5000,
2025-02-16,20,Buttermilk
2025-02-17,70,Milk
2025-02-17,25,ATM withdrawal charge
2025-02-17,840,Gas bottle
2025-02-17,210,
2025-02-17,350,Laptop maintenance
2025-02-17,300,Full thali
2025-02-17,190,Mattho
2025-02-18,5,
2025-02-18,50,Snacks
2025-02-20,10,Pan
2025-02-20,5,Pan
2025-02-20,1000,
2025-02-20,10,
2025-02-20,40,Pani puri
2025-02-20,5,
2025-02-20,5,
2025-02-20,120,Bhaji pulav
2025-02-21,35,"શ્રીફળ, અગરબત્તી"
2025-02-21,15,
2025-02-21,130,
2025-02-21,1080,YJ (Kathiyawadi penda)
2025-02-22,80,Icecream
2025-02-23,20,Ishan
2025-02-24,1500,
2025-02-24,60,
2025-02-24,2480,Power bank
2025-02-25,200,Airtel Bill 9913071189
2025-02-25,200,Airtel Bill 9712338989
2025-02-25,300,Airtel Bill 9712579779
2025-02-25,300,Airtel Bill 9426890029
2025-02-26,30,Milk
2025-02-26,200,Airtel Bill 8238338989
2025-02-27,2360,Electricity bill (8)
2025-02-27,40,Sugarcane juice
2025-02-27,100,
2025-02-28,100,Coke
2025-02-28,55,Sev
2025-03-01,240,
2025-03-01,1280,Electricity bill
2025-03-02,5,
2025-03-02,5,
2025-03-06,210,Vi Recharge 9925471189
2025-03-09,250,Sabudana
2025-03-10,20,Water bottle
2025-03-10,260,Bus ticket
2025-03-10,30,Riksha bhadu
2025-03-10,20,Limbu soda
2025-03-10,20,Limbu sharbat
2025-03-10,20,BRTS ticket
2025-03-10,20,Parking charges
2025-03-10,25,Orange juice
2025-03-10,55,Farali chevdo
2025-03-11,150,Sabudana
2025-03-11,600,SSJ
2025-03-12,2300,Buds
2025-03-12,60,Mava malai
2025-03-12,10,Tyre air check
2025-03-12,100,Dal bhat
2025-03-12,100,Mattho
2025-03-12,85,
2025-03-13,400,Fridge service
2025-03-15,710,Jio AirFiber Bill
2025-03-17,50,Toothbrush
2025-03-17,245,
2025-03-17,20,
2025-03-18,30,Activa sticker
2025-03-18,40,Face mask
2025-03-19,10,
2025-03-19,100,Mobile glass
2025-03-19,100,Buds cover
2025-03-19,55,Ice cream
2025-03-19,10,
2025-03-20,40,
2025-03-22,130,Full dish dinner
2025-03-22,60,Kadhi khichadi
2025-03-22,300,SJ
2025-03-22,95,American mattho
2025-03-23,100,Ishan
2025-03-23,120,Full dish dinner
2025-03-23,50,Kadhi khichadi
2025-03-25,10,Water bottle
2025-03-25,20,Shambhu kaka
2025-03-25,40,Farali chevdo
2025-03-27,15,
2025-03-27,700,Lenskart
2025-03-27,200,Airtel Bill 9913071189
2025-03-27,200,Airtel Bill 9712338989
2025-03-27,300,Airtel Bill 9712579779
2025-03-27,300,Airtel Bill 9426890029
2025-03-31,30,Farali chevdo
2025-03-31,50,
2025-04-01,20,
2025-04-01,580,Dhosa
2025-04-01,10,
2025-04-01,80,Ice cream
2025-04-02,320,Courier (to Vapi)
2025-04-02,220,
2025-04-02,1000,Nishan slippers
2025-04-02,200,Ice cream
2025-04-04,100,
2025-04-04,810,Gas bottle
2025-04-04,700,SSJ
2025-04-04,200,SSJ
2025-04-05,100,Mobile cover
2025-04-05,450,Bond paper
2025-04-05,30,Mamara
2025-04-06,30,Gas Bottle delivery
2025-04-06,220,Pav bhaji
2025-04-06,5,Fee
2025-04-07,200,SSJ
2025-04-07,580,Dhosa
2025-04-07,330,
2025-04-08,200,
2025-04-09,1290,SSJ savings
2025-04-09,775,PSJ savings
2025-04-09,100,Geyser power
2025-04-09,4000,D Mart
2025-04-11,50,Sprite
2025-04-16,710,Jio AirFiber Bill
2025-04-16,200,Airtel Bill 9712338989
2025-04-16,200,Airtel Bill 9913071189
2025-04-16,5000,SSJ cash
2025-04-16,100,"Pani puri, Sev"
2025-04-19,50,
2025-04-20,300,Airtel Bill 9426890029
2025-04-20,40,White cement
2025-04-22,500,
2025-04-24,300,Airtel Bill 9712579779
2025-04-24,50,
2025-04-24,100,Shambhu kaka riksha rent
2025-04-24,10,
2025-04-24,30,Vegetable (ગુંદા)
2025-04-24,105,
2025-04-25,140,Dinner (papa)
2025-04-25,130,Pav bhaji & pulao
2025-04-26,200,Bluetooth
2025-04-27,20,Buttermilk
2025-04-28,300,
2025-04-28,10,Water bottle
2025-04-28,100,
2025-04-28,40,
2025-04-29,105,"Poha, Snacks"
2025-04-29,105,Milk
2025-04-29,200,Mobile skin
2025-04-30,10,Water bottle
2025-05-01,85,Neel
2025-05-01,140,Milk
2025-05-02,50,Neel
2025-05-03,55,Soda
2025-05-03,50,Neel
2025-05-04,175,
2025-05-04,70,Nasto
2025-05-04,25,Nasto
2025-05-04,20,Pan
2025-05-04,100,Soda
2025-05-05,70,Shaving
2025-05-08,330,"Matches, Farali Chevdo, Bournvita"
2025-05-08,10,Xerox
2025-05-09,200,Airtel Bill 9913071189
2025-05-09,300,Ice cream
2025-05-09,2800,Trimmer (Bombay Shaving Company)
2025-05-13,20,
2025-05-14,300,Activa key
2025-05-17,500,
2025-05-17,710,Jio AirFiber Bill
2025-05-17,200,
2025-05-18,400,SSJ
2025-05-18,20,Buttermilk
2025-05-18,10,
2025-05-18,900,Electricity meter MCB
2025-05-18,5000,
2025-05-18,19000,Washing machine
2025-05-19,1250,SSJ
2025-05-19,250,
2025-05-20,5000,
2025-05-20,100,Soda bottle
2025-05-20,550,Washing machine accessories
2025-05-20,1360,Washing machine accessories
2025-05-20,20,
2025-05-21,100,Haircut
2025-05-21,250,Masala
2025-05-21,350,
2025-05-21,1200,"Maintenance (Apr, May, Jun)"
2025-05-21,80,
2025-05-22,20,
2025-05-23,20,
2025-05-23,200,Didi medicine
2025-05-23,210,Ice cream
2025-05-24,30,Soda
2025-05-25,300,"Pani puri ni puri, Jaljira, Sev"
2025-05-25,30,Soda
2025-05-25,300,YT Premium
2025-05-25,5,Transaction charge
2025-05-25,300,Debit card charge
2025-05-27,1600,Papa
2025-05-27,80,Tyre puncture
2025-05-27,100,Khaman
2025-05-30,215,Bus ticket
2025-05-30,10,Xerox
2025-05-31,40,Nasto
2025-06-02,380,Baa Medicine
2025-06-03,400,Hand gloves
2025-06-04,900,Hotel Sarvodaya
2025-06-05,30,Limbu soda
2025-06-08,300,Airtel Bill 9712579779
2025-06-08,300,Airtel Bill 9426890029
2025-06-08,70,SSJ
2025-06-08,115,
2025-06-11,350,Nasto
2025-06-12,3000,Neem Karoli Baba
2025-06-12,60,Nasto
2025-06-13,320,Ice cream
2025-06-16,70,Rapido parcel
2025-06-16,50,Riksha bhadu
2025-06-16,35,Buttermilk
2025-06-16,350,Bus ticket
2025-06-18,50,
2025-06-19,900,GJ DL renewal fee
2025-06-21,30,Farali chevdo
2025-06-22,710,Jio AirFiber Bill
2025-06-22,200,Airtel Bill 9712338989
2025-06-22,200,Airtel Bill 9913071189
2025-06-22,300,Airtel Bill 9426890029
2025-06-24,300,Airtel Bill 9712579779
2025-06-24,40,Ice cream
2025-06-25,300,YT Premium
2025-06-27,600,Coffee
2025-06-27,1300,Sandal
2025-06-27,400,Nasto
2025-06-27,500,Diesel
2025-06-28,1000,Petrol
2025-06-30,200,
2025-07-02,6000,Electricity bill
2025-07-02,200,Book
2025-07-03,5000,Activa Service
2025-07-03,7200,Naitik Bhai
2025-07-03,600,Naitik Bhai
2025-07-03,200,Dr. Bhautik (Oracle Hospital)
2025-07-03,4000,
2025-07-04,200,
2025-07-05,20,
2025-07-05,1000,"Cow ghee, bournvita, besan"
2025-07-05,1500,Activa tyre
2025-07-06,5,LinkedIn premium
2025-07-09,250,Mattho
2025-07-09,100,Papa
2025-07-10,350,Sugar
2025-07-10,250,
2025-07-10,450,"Activa oil, indicator switch"
2025-07-10,50,Banana
2025-07-11,305,
2025-07-12,200,Plug elbow and solution
2025-07-13,150,Pani puri ni puri
2025-07-13,250,
2025-07-13,100,Papa
2025-07-14,50,Sev
2025-07-15,710,Jio AirFiber Bill
2025-07-17,100,Ishan
2025-07-17,50,Wafer
2025-07-18,60,Bornvita + coffee
2025-07-18,450,Mobile back cover
2025-07-18,20,Water bottle
2025-07-19,120,Tiffin
2025-07-19,655,Train ticket for 21/07/2025
2025-07-20,70,
2025-07-20,20,Buttermilk
2025-07-20,30,
2025-07-20,1000,
2025-07-20,100,YJ
2025-07-20,610,Train ticket for 21/07/2025
2025-07-20,810,
2025-07-21,10,Parking charges
2025-07-21,30,Rapido
2025-07-21,175,McDonald's medium meal
2025-07-21,85,Train ticket for 21/07/2025
2025-07-21,50,
2025-07-21,15,Metro
2025-07-21,25,Rapido
2025-07-21,275,Hazelnut cold coffee
2025-07-21,15,Metro
2025-07-21,90,
2025-07-21,20,
2025-07-21,400,Train ticket for 21/07/2025
2025-07-21,10,Station ticket
2025-07-21,25,Rapido
2025-07-22,10,Pan
2025-07-22,20,Limbu soda
2025-07-22,60,Biskit
2025-07-22,10,Kinley soda
2025-07-22,1500,
2025-07-22,260,Milk and snacks
2025-07-22,200,Airtel Bill 9712338989
2025-07-23,350,Airtel Recharge 9925079779
2025-07-23,200,Airtel Bill 9913071189
2025-07-23,300,Airtel Bill 9426890029
2025-07-23,100,Papa
2025-07-24,50,
2025-07-24,40,Banana
2025-07-24,200,Vi Recharge 9925471189
2025-07-24,300,Airtel Bill 9712579779
2025-07-25,300,YT Premium
2025-07-29,20,
2025-07-29,40,Banana
2025-07-29,50,SSJ
2025-07-30,250,DCRS charges
2025-07-31,3500,Society member fee
2025-07-31,300,
2025-07-31,500,
2025-08-01,60,Farali chevdo
2025-08-01,335,
2025-08-02,40,Banana
2025-08-03,100,Guava
2025-08-04,80,Favi kwik
2025-08-04,65,Xerox
2025-08-04,20,Water bottle
2025-08-04,120,"Buttermilk, farali chevdo, crunchex"
2025-08-04,20,
2025-08-04,410,Wallet
2025-08-05,50,Bhungla
2025-08-05,800,Society maintenance
2025-08-07,1000,
2025-08-07,64000,PSJ college fee
2025-08-07,250,Lunch
2025-08-07,20,Water bottle
2025-08-07,20,Vada pav
2025-08-08,400,
2025-08-08,10,Tea
2025-08-08,20,Water bottle
2025-08-08,120,Parking charges
2025-08-10,150,Chequebook
2025-08-11,150,Tyre valve replacement
2025-08-11,10,Water bottle
2025-08-11,10,Tea
2025-08-12,500,
2025-08-12,50,Parking charges
2025-08-13,110,Bus ticket
2025-08-13,50,Parking charges
2025-08-14,200,Parking charges
2025-08-14,110,Bus ticket
2025-08-16,100,
2025-08-19,100,Farali chevdo
2025-08-19,15,"Tea, Potato wafer"
2025-08-19,195,D Mart
2025-08-21,60,Besan
2025-08-22,90,Tea
2025-08-23,190,Bus ticket
2025-08-23,600,
2025-08-23,100,Parking charges
2025-08-24,160,Bus ticket
2025-08-24,150,Washing machine accessories
2025-08-24,100,Haircut
2025-08-26,20,Tea
2025-08-30,250,"Milk, tea, nail cutter, earbud"
2025-09-01,150,"Milk, coffee"
2025-09-02,650,D Mart
2025-09-03,180,Jeera rice and dal fry
2025-09-04,100,Parking charges
2025-09-07,40,Parking charges
2025-09-07,200,Bus ticket
2025-09-08,50,Tea
2025-09-08,50,Vegetable
2025-09-08,200,"Buttermilk, lassi, orange pulpy"
2025-09-09,100,Sandwich
2025-09-09,200,Swiggy gift card
2025-09-10,500,Dinner
2025-09-10,100,Puff
2025-09-11,20,Tea
2025-09-14,4000,PG Rent
2025-09-14,40,Tea and snacks
2025-09-15,20,Tea
2025-09-15,500,
2025-09-15,120,Dinner
2025-09-15,20,
2025-09-16,20,Tea
2025-09-16,10,
2025-09-16,110,Dinner
2025-09-16,30,
2025-09-17,10,
2025-09-17,20,Tea
2025-09-17,80,Lunch
2025-09-17,40,
2025-09-17,100,Dinner
2025-09-17,200,Vi Recharge 9925471189
2025-09-18,485,Dinner
2025-09-18,10,
2025-09-18,20,Tea
2025-09-19,200,Bus ticket
2025-09-19,500,
2025-09-19,100,Parking charges
2025-09-19,20,Tea
2025-09-19,20,Tea
2025-09-19,10,
2025-09-20,150,Waghbakri tea
2025-09-21,2500,Training fees
2025-09-21,200,Bus ticket
2025-09-22,1170,"Mobile Cover, Mobile Holder for Activa"
2025-09-22,1000,
2025-09-22,10,Tea
2025-09-22,10,
2025-09-22,160,Dinner
2025-09-22,50,
2025-09-23,50,
2025-09-23,150,Fruits
2025-09-23,10,Tea
2025-09-23,200,
2025-09-24,150,Snacks
2025-09-24,10,Tea
2025-09-24,125,Airtel Bill 9712579779
2025-09-25,125,Airtel Bill 9426890029
2025-09-25,50,Ice cream
2025-09-25,300,YT Premium
2025-09-25,20,Buttermilk
2025-09-26,100,Parking charges
2025-09-26,110,Bus ticket
2025-09-27,100,Train ticket for 28/09/2025
2025-09-29,40,
2025-09-29,100,Mobile glass
2025-09-29,20,Tea
2025-09-30,10,Tea
2025-09-30,140,Sandwich
2025-10-01,40,Tea
2025-10-01,20,
2025-10-01,120,Dinner
2025-10-03,100,
2025-10-03,300,Dinner
2025-10-03,4000,Training fees
2025-10-03,7500,Rent
2025-10-03,150,Pani puri
2025-10-03,20,Tea
2025-10-04,50,Breakfast
2025-10-04,400,Lock kholava mate na
2025-10-04,30,
2025-10-05,100,
2025-10-06,20,Tea
2025-10-06,20,
2025-10-06,20,Tea
2025-10-07,20,Tea
2025-10-07,40,Tea
2025-10-08,20,Tea
2025-10-08,20,Tea
2025-10-08,50,
2025-10-09,20,Tea
2025-10-11,40,
2025-10-11,40,Tea
2025-10-12,200,YJ
2025-10-12,80,Breakfast
2025-10-12,710,Jio AirFiber Bill
2025-10-13,175,
2025-10-14,40,
2025-10-15,80,
2025-10-16,40,
2025-10-17,20,
2025-10-17,435,Miscellaneous
2025-10-18,50,
2025-10-18,100,
2025-10-20,40,
2025-10-22,120,Airtel Bill 9426890029
2025-10-22,200,Airtel Bill 9712338989
2025-10-24,120,Airtel Bill 9712579779
2025-10-25,30,SJ
2025-10-25,300,YT Premium
2025-10-25,200,Icecream
2025-10-26,120,Dinner
2025-10-26,10,Tyre air check
2025-10-27,40,Pani puri
2025-10-27,70,Frankie
2025-10-27,120,Dinner
2025-10-28,40,Tea + 20
2025-10-28,100,Pani puri
2025-10-28,1000,
2025-10-28,140,Water
2025-10-29,40,Tea + 20
2025-10-29,70,Nasto
2025-10-31,125,Lunch
2025-10-31,40,
2025-11-01,120,Lunch
2025-11-01,40,Nasto
2025-11-01,120,Dinner
2025-11-01,20,
2025-11-02,120,Dinner
2025-11-02,1000,
2025-11-02,1000,
2025-11-03,1000,
2025-11-03,50,Milk + Toothpaste
2025-11-03,10,Gemini Pro
2025-11-03,300,
2025-11-03,70,Sugar + Milk
2025-11-04,750,Conference registration
2025-11-04,120,Dinner
2025-11-04,7500,PSJ Rent
2025-11-05,40,
2025-11-05,15,Buttermilk
2025-11-05,20,Milk
2025-11-05,215,Dal Makhni Jira Rice
2025-11-05,10,Parking charges
2025-11-06,10,Milk
2025-11-06,365,Nasto
2025-11-06,120,Dinner
2025-11-07,10,Milk
2025-11-07,120,Dinner
2025-11-08,20,Train ticket
2025-11-08,70,Laundry
2025-11-08,55,Parking charges
2025-11-08,20,Milk
2025-11-08,710,Jio AirFiber Bill
2025-11-09,70,Rapido
2025-11-09,8000,
2025-11-09,500,Raj Khunt Sister marriage
2025-11-09,75,Train ticket
2025-11-09,40,Soda
2025-11-10,140,Coffee
2025-11-10,500,YJ
2025-11-10,120,Lunch
2025-11-10,100,
2025-11-11,20,
2025-11-11,20,Vegetable
2025-11-11,20,Milk
2025-11-11,40,
2025-11-11,110,Dinner
2025-11-11,195,
2025-11-12,40,Milk
2025-11-12,200,Vi Recharge 9925471189
2025-11-12,100,Ishan
2025-11-12,20,
2025-11-12,10,
2025-11-12,20,Milk
2025-11-13,5,Jaljira
2025-11-13,10,Maggie masala
2025-11-13,30,Maggie
2025-11-13,20,Soda
2025-11-13,20,Milk
2025-11-13,20,Vegetable
2025-11-13,20,Personal
2025-11-14,20,
2025-11-14,40,Coco
2025-11-14,120,Water bottle
2025-11-14,175,
2025-11-14,600,Wallet
2025-11-15,200,Airtel Bill 9712338989
2025-11-15,650,Dinner
2025-11-15,500,YJ
2025-11-15,120,Airtel Bill 9913071189
2025-11-15,20,Vegetables
2025-11-15,20,Milk
2025-11-15,20,Aloo Sev
2025-11-15,50,Sprite
2025-11-16,200,YJ
2025-11-16,500,YJ
2025-11-16,20,Milk
2025-11-16,130,Nasto
2025-11-17,60,Personal
2025-11-17,40,Milk
2025-11-17,30,Nasto
2025-11-17,20,Milk
2025-11-18,40,Milk
2025-11-18,120,Angel One
2025-11-18,10,Water refilling
2025-11-19,20,Milk
2025-11-19,60,Personal
2025-11-19,15,Buttermilk
2025-11-20,130,Nasto
2025-11-20,25,Limbu Soda
2025-11-20,120,Airtel Bill 9426890029
2025-11-20,110,Nasto
2025-11-20,80,Nasto
2025-11-20,50,Chocolate
2025-11-20,20,Milk
2025-11-21,170,Lunch
2025-11-21,10,Water refilling
2025-11-21,10,Ajma
2025-11-22,25,Limbu Soda
2025-11-22,30,Tea
2025-11-22,140,Lunch 
2025-11-22,35,Coffee
2025-11-22,20,Personal
2025-11-22,120,Dinner
2025-11-23,10,Personal
2025-11-23,20,Personal
2025-11-23,125,Dinner
2025-11-23,500,YJ
2025-11-23,20,Vegetables
2025-11-23,15,Buttermilk
2025-11-23,200,YJ
2025-11-24,40,Personal
2025-11-24,190,Lunch 
2025-11-24,120,Airtel Bill 9712579779
2025-11-24,20,Tea
2025-11-25,20,Milk
2025-11-25,130,Lunch 
2025-11-25,50,Coffee
2025-11-25,110,Dinner
2025-11-25,300,YT Premium
2025-11-25,40,Personal
2025-11-26,30,Soda
2025-11-26,20,Tea
2025-11-26,20,Personal
2025-11-26,170,
2025-11-26,30,
2025-11-26,50,
2025-11-26,120,Dinner
2025-11-27,20,
2025-11-27,20,Milk
2025-11-27,110,Dinner
2025-11-28,20,Milk
2025-11-28,170,
2025-11-28,35,
2025-11-29,210,
2025-11-29,450,
2025-11-30,60,
2025-11-30,490,
2025-12-01,300000,YJ
2025-12-01,40,
2025-12-01,10000,Laptop Advance Croma
2025-12-01,38000,YJ
2025-12-02,86000,Chintan Patel (Laptop Payment)
2025-12-02,10,BOB Charges
2025-12-02,100,Chintan Patel
2025-12-02,450,Laptop Lamination
2025-12-02,1400,Mobile Cover and Glass
2025-12-02,3000,
2025-12-03,5,Google Play
2025-12-03,500,YJ
2025-12-03,60,
2025-12-03,35,
2025-12-05,30,Gas Bottle Delivery
2025-12-05,1000,RJ (YJ)
2025-12-05,500,YJ
2025-12-06,500,YJ
2025-12-06,90,SJJ Dinner
2025-12-06,20,Kinely Soda
2025-12-07,50,SJJ Aadhaar PVC Card
2025-12-07,710,Jio AirFiber Bill
2025-12-08,80,Fruits
2025-12-08,30,Farali Chevdo
2025-12-08,10,Tyre Air Check
2025-12-08,15,Buttermilk
2025-12-09,60,Activa accessory repairing
2025-12-09,225,YJ
2025-12-09,20,Milk
2025-12-09,5,Xerox
2025-12-10,35,Buttermilk
2025-12-10,120,Dinner
2025-12-10,5,Google Play
2025-12-11,300,Mosquito racket
2025-12-11,7500,Rent
2025-12-12,40,Sprite
2025-12-12,70,Franky
2025-12-12,800,Library fees
2025-12-12,40,Panipuri
2025-12-12,20,Milk
2025-12-13,35,Milk
2025-12-13,15,Buttermilk
2025-12-13,200,Airtel Bill 9712338989
2025-12-13,120,Airtel Bill 9913071189
2025-12-13,300,Laptop keyboard guard
2025-12-13,50,Navy blue jacket
2025-12-14,30,Soda
2025-12-14,60,Vegetables and fruits
2025-12-14,2000,YJ
2025-12-14,500,Sim card
2025-12-15,40,Laundry
2025-12-16,35,Milk
2025-12-16,120,Dinner
2025-12-16,160,Savings
2025-12-17,35,Milk
2025-12-17,90,Dinner
2025-12-18,70,Nasto
2025-12-18,20,Pool 🎱 
2025-12-18,35,Milk
2025-12-18,110,Nasto
2025-12-19,235,Fuel
2025-12-19,115,Parking charges
2025-12-19,60,Train ticket 
2025-12-19,70,Rapido
2025-12-20,250,Krish sister marriage 
2025-12-20,40,Soda
2025-12-20,80,Nasto khetlaapa
2025-12-20,25,Soda
2025-12-20,10,Parking charges
2025-12-20,50,Train ticket 
2025-12-20,80,Nasto
2025-12-20,50,Train ticket 
2025-12-21,20,Soda
2025-12-25,60,Rapido
2025-12-25,300,YT Premium
2025-12-26,880,Laptop Table
2025-12-27,40,Rapido
2025-12-27,30,Rapido
2025-12-27,150,Parking charges
2025-12-27,35,Milk
2025-12-27,59000,PSJ (NUV Fees)
2025-12-28,15,Buttermilk
2025-12-28,1200,Miscellaneous
2025-12-28,120,Airtel Bill 9426890029
2025-12-28,410,Fuel
2025-12-29,220,Pool 🎱 
2025-12-29,35,Milk
2025-12-30,110,Dinner
2025-12-31,35,Milk
2025-12-31,90,Snacks
2025-12-31,40,Soda
2026-01-01,10,Maggie Masala
2026-01-01,35,Milk
2026-01-01,100,Snacks
2026-01-01,30,Maggie
2026-01-02,35,Milk
2026-01-02,220,ACV
2026-01-02,1200,Domain
2026-01-02,850,Activa seat cover
2026-01-02,300,Grocery
2026-01-03,700,Pizza 
2026-01-04,170,Nasto
2026-01-04,500,Punjabi dinner
2026-01-05,75,Jio Bill 9712579779
2026-01-05,70,Dinner
2026-01-05,90,Ice cream
2026-01-06,20,Milk
2026-01-06,10,Personal 
2026-01-06,380,Fuel
2026-01-07,100,Nasto
2026-01-08,1000,Shirt
2026-01-09,10,
2026-01-09,15,Personal 
2026-01-09,40,Tea
2026-01-09,200,Tyre puncture
2026-01-10,35,
2026-01-10,900,Lock breaking and repairing
2026-01-10,20,Personal 
2026-01-10,120,Dinner
2026-01-11,105,
2026-01-11,20,Vegetables
2026-01-11,20,
2026-01-11,20,
2026-01-12,80,Pani Puri
2026-01-13,40,
2026-01-13,60,Train Ticket
2026-01-13,120,Manchurian
2026-01-13,85,
2026-01-14,60,
2026-01-16,7500,Rent
2026-01-17,400,Petrol
2026-01-17,80,
2026-01-17,30,
2026-01-18,80,Marie Gold
2026-01-18,10,Medical
2026-01-18,120,Airtel Bill 9426890029
2026-01-18,500,YJ
2026-01-18,1000,YJ
2026-01-19,120,Pani Puri
2026-01-19,30,Lemon + potato
2026-01-19,20,Soda
2026-01-19,585,Cooling Pad
2026-01-19,100,Fruit juice
2026-01-20,15,Vegetables
2026-01-20,10,Iron
2026-01-20,225,Mobile Stand
2026-01-20,30,
2026-01-21,600,YJ
2026-01-21,10,Personal
2026-01-21,75,Grocery and Milk
2026-01-21,20,Vegetables
2026-01-22,20,Cold drink
2026-01-23,20,Snacks
2026-01-23,40,Tea
2026-01-24,35,Ravo
2026-01-24,70,Rapido
2026-01-24,290,Engine Oil
2026-01-24,130,Nasto
2026-01-24,85,Parking Charges
2026-01-24,450,Fuel
2026-01-25,300,YT Premium
2026-01-25,50,YJ
2026-01-27,15,Buttermilk
2026-01-27,140,Bus ticket
2026-01-27,10,Personal 
2026-01-28,10,Personal
2026-01-28,120,Pani Puri
2026-01-28,40,Personal
2026-01-29,10,Personal
2026-01-29,300,Earphones
2026-01-30,10,Personal 
2026-01-30,250,Hair cut
2026-01-30,300,Hackathon (Thinkerthon)
2026-01-30,420,Pizza
2026-01-31,50,Nasto
2026-01-31,100,Auto rent
2026-02-03,100,Medicine
2026-02-05,200,Jio Bill 9712579779
2026-02-05,200,VI Recharge 9925471189
2026-02-06,710,Jio AirFiber Bill
2026-02-06,100,Nasto
2026-02-06,400,Airtel Bill 9712338989
2026-02-07,195,Jio Recharge 9274579779
2026-02-07,500,Shehzadbhai Jio AirFiber
2026-02-07,375,Nasto SSJ
2026-02-09,37,Rapido
2026-02-09,58,Rapido
2026-02-09,140,Lunch
2026-02-09,10,Personal
2026-02-09,35,Milk
2026-02-11,30,
2026-02-11,160,
2026-02-12,100,Premvati
2026-02-12,105,Nasto
2026-02-12,7500,Rent
2026-02-13,200,Chocolate
2026-02-13,150,Juice
2026-02-13,100,Nasto
2026-02-14,120,Bus Ticket
2026-02-14,100,
2026-02-16,15,Buttermilk
2026-02-16,100,Vegetables
2026-02-16,200,Bus ticket
2026-02-16,415,Fuel
2026-02-17,150,Juice
2026-02-17,30,Vegetables
2026-02-17,15,Buttermilk
2026-02-17,2500,BRC - NZM Train
2026-02-17,2600,NZM - BRC Train
2026-02-18,40,
2026-02-18,110,Premvati
2026-02-18,70,"Pani puri, Sev puri"
2026-02-19,120,Taxi
2026-02-20,45,
2026-02-20,550,Lunch
2026-02-20,185,Taxi
2026-02-20,10,Bank charges
2026-02-20,310,Dinner
2026-02-20,145,Nasto
2026-02-21,150,Premvati
2026-02-21,155,Taxi
2026-02-22,135,
2026-02-22,130,Breakfast
2026-02-23,310,Fruits
2026-02-23,45,Farali chevdo
2026-02-23,120,Dinner
2026-02-24,140,Dinner
2026-02-24,30,Soda
2026-02-24,85,Personal
2026-02-24,120,Airtel Bill 9426890029
2026-02-24,1500,Hotel rent (Prem)
2026-02-25,90,Sandwitch
2026-02-25,165,Juice
2026-02-25,130,Nasto
2026-02-26,115,Breakfast
2026-02-26,100,Juice
2026-02-26,35,Milk
2026-02-26,140,Dinner
2026-02-28,250,Sev Usal
2026-03-01,1035,Activa Service
2026-03-02,25,Curd
2026-03-02,50,Juice
2026-03-02,400,Fuel
2026-03-03,50,Thepla
2026-03-03,20,Tea
2026-03-03,90,Icecream
2026-03-03,40,Banana
2026-03-04,70,Vadapav and Dabeli
2026-03-04,1000,YJ
2026-03-04,125,Mathho and Buttermilk
2026-03-04,70,Frankie
2026-03-05,7500,Feb 2026 Rent
2026-03-05,85,Breakfast
2026-03-05,30,Coffee
2026-03-05,115,Snacks
2026-03-05,50,Sprite
2026-03-05,20,Personal
2026-03-05,100,Juice
2026-03-07,5,Mobile cover 
2026-03-07,15,Buttermilk
2026-03-07,60,Juice
2026-03-07,200,Jio Bill 9712579779
2026-03-07,35,Milk
2026-03-08,50,Personal
2026-03-08,60,Juice 
2026-03-08,210,Fuel
2026-03-08,200,Vegetables
2026-03-09,20,Tea
2026-03-09,120,Dinner
2026-03-10,60,Juice
2026-03-10,70,
2026-03-10,60,Coconut water 
2026-03-11,165,Breakfast and lunch
2026-03-11,35,Milk
2026-03-11,60,Juice
2026-03-11,210,Fuel
2026-03-12,20,Tea
2026-03-12,130,Dinner
2026-03-12,50,Personal
2026-03-13,40,Buttermilk and curd
2026-03-13,25,Breakfast
2026-03-14,40,
2026-03-14,14000,Diposit
2026-03-14,20,Buttermilk
2026-03-15,360,Airtel Bill 9913071189
2026-03-15,10,Personal
2026-03-16,20,
2026-03-16,200,
2026-03-16,60,Juice
2026-03-17,40,Fruits
2026-03-17,60,Burger
2026-03-17,45,Buttermilk
2026-03-18,125,Nasto
2026-03-18,500,Fuel
2026-03-18,120,Dinner
2026-03-18,385,Movie ticket (21-03-2026)
2026-03-19,15,Buttermilk
2026-03-19,50,Juice
2026-03-19,200,Airtel Bill 9712338989
2026-03-19,120,Airtel Bill 9426890029
2026-03-19,10,Tea
2026-03-19,55,Breakfast
2026-03-20,10,Milk
2026-03-20,90,Dinner
2026-03-20,20,Personal
2026-03-21,90,Groceries
2026-03-21,200,Bus Ticket
2026-03-21,100,Parking
2026-03-21,120,Dinner
2026-03-21,30,
2026-03-21,70,Nasto
2026-03-23,90,Juice 
2026-03-23,20,Personal
2026-03-23,65,Groceries 
2026-03-24,35,
2026-03-24,10,Milk
2026-03-24,150,Shifting
2026-03-25,300,YT Premium
2026-03-25,95,Rapido
2026-03-25,200,Bus Ticket
2026-04-04,3000,Y J (Bhavesh Uncle Ravani)
2026-04-04,200,Tirth Italiya
2026-04-05,200,Jio Bill 9712579779
2026-04-05,350,New Sim Card
2026-04-06,80,Lunch Nasto
2026-04-06,115,Bus Ticket
2026-04-06,150,SJJ
2026-04-06,55,Rapido
2026-04-06,50,Personal
2026-04-06,150,Auto rent
2026-04-06,50,Taxi rent
2026-04-07,400,Y J
2026-04-07,4500,Y J (IGJ)
2026-04-07,30,Breakfast
2026-04-07,190,Lunch Nasto
2026-04-07,10,Gas Repairing
2026-04-08,1000,Saving
2026-04-08,120,Dinner
2026-04-08,20,Personal
2026-04-08,200,Duplicate key
2026-04-08,200,Bank Charges
2026-04-08,180,Lunch Nasto
2026-04-08,40,Breakfast
2026-04-09,35,Milk
2026-04-09,20,Personal
2026-04-09,60,Panipuri
2026-04-09,40,Sugercan Juice
2026-04-09,710,Jio AirFiber Bill
2026-04-09,120,Bhajiya
2026-04-09,30,Buttermilk
2026-04-10,7000,April Rent
2026-04-10,120,Dinner
2026-04-10,20,Personal
2026-04-11,200,Y J (Blade)
2026-04-11,100,Bike wash
2026-04-11,20,Buttermilk
2026-04-12,1110,Fuel
2026-04-12,40,Nasto
2026-04-13,20,Personal
2026-04-13,20,Dahi
2026-04-13,15,Buttermilk
2026-04-14,20,Nasto
2026-04-15,50,Gathiya
2026-04-16,20,Personal
2026-04-16,15,Buttermilk
2026-04-16,60,Laundry
2026-04-16,15,Buttermilk
2026-04-16,35,Poha
2026-04-17,160,Ice cream and chocolate
2026-04-17,200,Nasto
2026-04-17,20,Soda
2026-04-18,15,Buttermilk
2026-04-18,60,Personal
2026-04-18,160,Nasto
2026-04-19,30,Buttermilk
2026-04-19,25,Buttermilk
2026-04-19,25,Sugar
2026-04-19,60,Vegetable
2026-04-20,20,Tea
2026-04-20,200,Dinner 
2026-04-21,500,Y J
2026-04-22,20,Personal
2026-04-22,20,Soda
2026-04-22,2000,Uchhina return
2026-04-22,200,Dinner
2026-04-22,800,SJJ
2026-04-23,300,Hair cut
2026-04-23,20,Personal
2026-04-23,250,Bank Charges
2026-04-23,15,Buttermilk
2026-04-23,200,Ice-cream
2026-04-23,120,Dinner
2026-04-23,1000,Y J
2026-04-23,60,Breakfast
2026-04-24,30,Buttermilk
2026-04-24,120,Dinner
2026-04-24,20,Personal
2026-04-25,60,Breakfast
2026-04-25,10,Water refill
2026-04-25,165,Nasto
2026-04-25,5000,Jignesh bhai
2026-04-25,300,YT Premium
2026-04-25,120,Dinner
2026-04-25,10,Personal
2026-04-26,20,SSJ
2026-04-26,10,Water refill
2026-04-26,30,Buttermilk
2026-04-28,120,Dinner
2026-04-29,90,Vegetable
2026-04-29,20,Laundry
2026-04-29,100,Grocery
2026-04-29,120,Airtel Bill 9913071189
2026-04-29,300,Airtel Recharge 9925079779
2026-04-29,120,Airtel Bill 9426890029
2026-04-29,30,Buttermilk
2026-04-30,120,Dinner
2026-04-30,200,Vi Recharge 9925471189
2026-05-01,35,Milk
2026-05-01,260,Grocery
2026-05-02,80,Parking Charges
2026-05-02,420,Bus Ticket
2026-05-02,500,Y J
2026-05-02,55,Rapido
2026-05-02,1000,Y J
2026-05-02,200,Y J
2026-05-04,120,Bus Ticket
2026-05-04,10,Water refill
2026-05-04,15,Wire tap
2026-05-05,60,Nasto
2026-05-05,100,Nasto Canteen
2026-05-05,15,Buttermilk
2026-05-05,400,Zomato Dinner
2026-05-05,180,Jio Bill 9712579779
2026-05-05,15,Buttermilk
2026-05-06,150,Lunch Nasto
2026-05-06,280,Nasto
2026-05-06,3000,Y J
2026-05-06,120,Dinner
2026-05-06,50,Personal
2026-05-06,15,Buttermilk
2026-05-06,150,Dinner
2026-05-07,150,Dinner
2026-05-07,50,Personal
2026-05-08,25,Personal
2026-05-08,20,Soda
2026-05-08,50,Nasto
2026-05-09,120,Airtel Bill 9913071189
2026-05-09,50,Lenskart Gold Max membership
2026-05-09,400,Zomato Dinner
2026-05-09,10,Water refill
2026-05-09,60,Nasto
2026-05-09,710,Jio AirFiber Bill
2026-05-09,7600,May Rent - May Bill - April Bill
2026-05-10,700,USB Hub
2026-05-10,160,Lunch
2026-05-10,30,Personal
2026-05-10,15,Soda
2026-05-10,370,Ice cream
2026-05-10,130,Dinner
2026-05-11,10,Personal
2026-05-11,10,Water refill
2026-05-11,130,Dinner
2026-05-12,130,Dinner
2026-05-12,185,
2026-05-12,700,Launched Edutec
2026-05-12,90,Vegetable
2026-05-12,30,Buttermilk
2026-05-12,35,Ravo
2026-05-13,20,Tea
2026-05-13,130,Lunch
2026-05-13,30,Buttermilk
2026-05-14,130,Dinner
2026-05-14,20,Personal
2026-05-14,200,Grocery
2026-05-14,115,Lunch
2025-12-13,50,Laundry
2025-12-15,40,Laundry
2025-12-16,35,Milk
2025-12-16,120,Dinner
2025-12-16,160,Savings
2025-12-17,35,Milk
2025-12-17,90,Dinner
2025-12-18,70,Nasto
2025-12-18,20,Pool 🎱 
2025-12-18,35,Milk
2025-12-18,110,Nasto
2025-12-19,235,Fuel
2025-12-19,115,Parking charges
2025-12-19,60,Train ticket 
2025-12-19,70,Rapido
2025-12-20,250,Krish sister marriage 
2025-12-20,40,Soda
2025-12-20,80,Nasto khetlaapa
2025-12-20,25,Soda
2025-12-20,10,Parking charges
2025-12-20,50,Train ticket 
2025-12-20,80,Nasto
2025-12-20,50,Train ticket 
2025-12-21,20,Soda
2025-12-25,60,Rapido
2025-12-25,300,YT Premium
2025-12-26,880,Laptop Table
2025-12-27,40,Rapido
2025-12-27,30,Rapido
2025-12-27,150,Parking charges
2025-12-27,35,Milk
2025-12-27,59000,PSJ (NUV Fees)
2025-12-28,15,Buttermilk
2025-12-28,1200,Miscellaneous
2025-12-28,120,Airtel Bill 9426890029
2025-12-28,410,Fuel
2025-12-29,220,Pool 🎱 
2025-12-29,35,Milk
2025-12-30,110,Dinner
2025-12-31,35,Milk
2025-12-31,90,Snacks
2025-12-31,40,Soda
2026-05-15,390,Dinner
2026-05-15,140,Ice cream
2026-05-16,180,Project parts
2026-05-16,300,Project parts
2026-05-17,130,Breakfast
2026-05-17,10,Milk
2026-05-17,30,Buttermilk
2026-05-18,20,
2026-05-18,130,Dinner
2026-05-18,30,Sugercan Juice
2026-05-18,20,Personal
2026-05-18,20,Tea
2026-05-18,150,Ice cream
2026-05-19,20,Personal
2026-05-19,170,Dinner
2026-05-19,60,Breakfast
2026-05-19,20,Tea
2026-05-19,20,Soda
    """.trimIndent()

    val block3 = """
2025-01-01,300,Eyeglasses
2025-01-01,500,Eyeglasses
2025-01-01,130,
2025-01-01,15,
2025-01-01,5,
2025-01-01,45,
2025-01-01,540,
2025-01-02,6000,
2025-01-03,500,
2025-01-04,100,Udhar from RJ
2025-01-07,13000,Home
2025-01-07,200,
2025-01-08,15000,Home
2025-01-11,2500,
2025-01-11,400,Gautam Bhai
2025-01-18,500,
2025-01-21,20,
2025-01-23,200,
2025-01-23,100,Aunty (Lock key)
2025-01-24,14000,
2025-01-24,500,
2025-01-24,2000,
2025-01-26,50,
2025-01-26,50,
2025-01-27,2500,
2025-01-28,800,
2025-01-29,30,
2025-01-29,500,RJ
2025-01-29,70,
2025-01-30,120,
2025-01-30,5,Parth PF
2025-01-30,1000,
2025-01-31,1000,
2025-02-02,250,Naitik Bhai
2025-02-02,3500,Ishan
2025-02-02,8000,
2025-02-04,18000,
2025-02-11,200,Hansa Masi
2025-02-12,240,
2025-02-14,200,
2025-02-15,10000,
2025-02-17,1000,
2025-02-21,1300,
2025-02-22,1500,Home
2025-02-24,980,
2025-02-24,1500,
2025-02-25,1000,
2025-02-25,2000,
2025-02-26,190,Ishan
2025-02-26,160,
2025-02-27,500,
2025-03-01,1500,B K Ravani
2025-03-05,2000,Home
2025-03-06,200,Ishan
2025-03-09,20,
2025-03-10,1000,
2025-03-10,500,
2025-03-11,1000,
2025-03-12,315,
2025-03-13,700,Uncle for Nishan slippers
2025-03-14,2000,
2025-03-15,500,Papa
2025-03-19,2000,
2025-03-27,450,
2025-03-27,60,
2025-03-28,10000,Home
2025-04-09,770,
2025-04-09,6500,Home
2025-04-09,285,SSJ
2025-04-16,1700,
2025-04-18,170,
2025-04-25,500,
2025-04-28,400,
2025-05-03,2000,
2025-05-08,1000,
2025-05-09,2800,Bombay Shaving Company
2025-05-15,50,SSJ
2025-05-16,16000,Maharshi Villa
2025-05-18,20000,
2025-05-23,2000,
2025-05-25,600,
2025-05-25,300,
2025-05-30,2000,SSJ
2025-06-08,5000,
2025-06-19,30,SSJ
2025-06-19,1000,GJ (Chetan Bhai)
2025-06-21,1800,
2025-06-24,300,Didi
2025-06-26,500,Uncle
2025-06-27,3500,
2025-06-30,500,SSJ
2025-07-02,5500,PF
2025-07-02,7000,PF
2025-07-03,12000,
2025-07-05,1000,
2025-07-10,500,SSJ
2025-07-10,1000,
2025-07-13,500,
2025-07-14,200,
2025-07-15,4000,
2025-07-15,1000,
2025-07-20,295,Refund of train ticket for 21/07/2025
2025-07-21,5000,
2025-07-21,515,Refund of train ticket for 21/07/2025
2025-07-25,500,SSJ
2025-07-29,150,
2025-07-30,2000,Society maintenance
2025-07-30,2700,Society member fee
2025-07-31,500,
2025-08-01,500,
2025-08-06,2500,
2025-08-06,60000,PSJ college fee
2025-08-07,2270,PSJ savings
2025-08-08,1500,
2025-08-08,100,Parking charges
2025-08-12,1500,
2025-08-14,1000,
2025-08-17,2000,
2025-09-07,1000,
2025-09-14,5000,
2025-09-17,1500,
2025-09-21,4000,
2025-09-28,2000,
2025-10-03,5000,
2025-10-03,6000,PF
2025-10-12,1000,
2025-10-19,1000,
2025-10-20,5000,
2025-10-24,1000,
2025-10-25,1000,
2025-10-26,5000,
2025-11-01,5000,PF
2025-11-01,5000,Rent
2025-11-02,10000,YJ
2025-11-14,1000,YJ
2025-11-14,2000,Neel
2025-11-18,2000,SSJ
2025-11-21,2500,YJ
2025-11-28,100,Cream Commission 
2025-11-30,250,YJ
2025-12-01,426070,Gold Loan
2025-12-02,15000,YJ
2026-01-13,490,YJ
2026-01-17,10000,YJ
2026-01-17,5000,SSJ
2026-01-21,1000,YJ
2026-01-27,2000,YJ
2026-02-06,2000,YJ
2026-02-08,1100,SSJ
2026-02-12,12000,YJ
2026-02-17,10000,YJ
2026-03-04,5000,YJ
2026-03-06,3000,YJ
2026-03-09,1000,SSJ
2026-03-17,5000,YJ
2026-03-30,5000,YJ
2026-04-03,6300,Deposite Return
2026-04-06,19000,Y J
2026-04-08,5000,Y J
2026-04-11,1200,Y J
2026-04-18,3000,Y J
2026-04-19,3000,Y J
2026-04-20,10000,Y J
2026-05-03,5000,Y J
2026-05-06,10000,Y J
    """.trimIndent()

    val block4 = """
2026-04-07,451.92,Fuel
2026-04-18,130.0,Dinner
    """.trimIndent()

    fun parseBlock(b: String): List<Pair<Double, String>> {
      return b.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
          val parts = line.split(",")
          val amt = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
          val desc = parts.getOrNull(2) ?: ""
          amt to desc
        }.toList()
    }

    val b1 = parseBlock(block1)
    val b2 = parseBlock(block2)
    val b3 = parseBlock(block3)
    val b4 = parseBlock(block4)

    val expenseSum = b1.sumOf { it.first } + b2.sumOf { it.first } + b4.sumOf { it.first }
    val incomeSum = b3.sumOf { it.first }

    println("DIAGNOSTIC: b1 sum: ${b1.sumOf { it.first }}")
    println("DIAGNOSTIC: b2 sum: ${b2.sumOf { it.first }}")
    println("DIAGNOSTIC: b3 sum: ${b3.sumOf { it.first }}")
    println("DIAGNOSTIC: b4 sum: ${b4.sumOf { it.first }}")
    println("DIAGNOSTIC: raw incomeSum: $incomeSum, raw expenseSum: $expenseSum")
    println("DIAGNOSTIC: Net direct: ${incomeSum - expenseSum}")

    // Now, let's identify "family member transactions".
    // Known initials / names from user text: "YJ", "Y J", "SSJ", "SJJ", "PSJ", "RJ", "Ishan", "Baa", "Papa", "Didi", "Chintan Patel", "Home"
    val familyNames = listOf("YJ", "Y J", "SSJ", "SJJ", "PSJ", "RJ", "Ishan", "Baa", "Papa", "Didi", "Chintan Patel", "Home")
    
    fun isFamily(desc: String): Boolean {
      val d = desc.lowercase()
      return familyNames.any { f -> 
        val fLow = f.lowercase()
        d == fLow || d.startsWith(fLow + " ") || d.endsWith(" " + fLow) || d.contains(" " + fLow + " ") || d.contains("(" + fLow) || d.contains(fLow + ")")
      }
    }

    val b3Family = b3.filter { isFamily(it.second) }
    val b3NonFamily = b3.filter { !isFamily(it.second) }

    val b2Family = b2.filter { isFamily(it.second) }
    val b2NonFamily = b2.filter { !isFamily(it.second) }

    val b1Family = b1.filter { isFamily(it.second) }
    val b1NonFamily = b1.filter { !isFamily(it.second) }

    val b4Family = b4.filter { isFamily(it.second) }
    val b4NonFamily = b4.filter { !isFamily(it.second) }

    val totalFamilyExp = b1Family.sumOf { it.first } + b2Family.sumOf { it.first } + b4Family.sumOf { it.first }
    val totalNonFamilyExp = b1NonFamily.sumOf { it.first } + b2NonFamily.sumOf { it.first } + b4NonFamily.sumOf { it.first }

    val totalFamilyInc = b3Family.sumOf { it.first }
    val totalNonFamilyInc = b3NonFamily.sumOf { it.first }

    println("DIAGNOSTIC: totalFamilyInc = $totalFamilyInc")
    println("DIAGNOSTIC: totalNonFamilyInc = $totalNonFamilyInc")
    println("DIAGNOSTIC: totalFamilyExp = $totalFamilyExp")
    println("DIAGNOSTIC: totalNonFamilyExp = $totalNonFamilyExp")

    // Let's print different combinations using different definitions of family
    // We want to find a combination of b1, b2, b3, b4 that yields 86275.0 or -2975.0
    // e.g. some net of incomes and expenses.
    // Let's calculate:
    val sumB1 = b1.sumOf { it.first }
    val sumB2 = b2.sumOf { it.first }
    val sumB3 = b3.sumOf { it.first }
    val sumB4 = b4.sumOf { it.first }

    // Let's print out what combinations sum to exactly -2975 and 86275!
    // Let's look at the difference: 86275 - (-2975) = 89250
    // Is there some transaction or subset of transactions whose sum is 89250?
    // Let's search all items in b1, b2, b3, b4 that could sum to 89250 or 86275 or 2975
    val allItems = (b1 + b2 + b3 + b4).distinctBy { it.first.toString() + "_" + it.second }
    println("DIAGNOSTIC - Searching for single or double items that sum to 89250:")
    for (i in allItems.indices) {
        val item1 = allItems[i]
        if (Math.abs(item1.first - 89250.0) < 1.0) {
            println("Single item matching 89250: ${item1.first} - ${item1.second}")
        }
        for (j in i + 1 until allItems.size) {
            val item2 = allItems[j]
            if (Math.abs(item1.first + item2.first - 89250.0) < 1.0) {
                println("Two items sum to 89250: ${item1.first} (${item1.second}) + ${item2.first} (${item2.second})")
            }
            if (Math.abs(item1.first - item2.first - 89250.0) < 1.0) {
                println("Two items diff to 89250: ${item1.first} (${item1.second}) - ${item2.first} (${item2.second})")
            }
        }
    }

    // For b3 (Income):
    val b3YJ = b3.filter { it.second.lowercase().contains("yj") }.sumOf { it.first }
    val b3SSJ = b3.filter { it.second.lowercase().contains("ssj") || it.second.lowercase().contains("sjj") }.sumOf { it.first }
    val b3PSJ = b3.filter { it.second.lowercase().contains("psj") }.sumOf { it.first }
    val b3RJ = b3.filter { it.second.lowercase().contains("rj") }.sumOf { it.first }
    val b3Ishan = b3.filter { it.second.lowercase().contains("ishan") }.sumOf { it.first }
    val b3OtherFam = totalFamilyInc - (b3YJ + b3SSJ + b3PSJ + b3RJ + b3Ishan)
    
    // For b2 (Expense):
    val b2YJ = b2.filter { it.second.lowercase().contains("yj") }.sumOf { it.first }
    val b2SSJ = b2.filter { it.second.lowercase().contains("ssj") || it.second.lowercase().contains("sjj") }.sumOf { it.first }
    val b2PSJ = b2.filter { it.second.lowercase().contains("psj") }.sumOf { it.first }
    val b2RJ = b2.filter { it.second.lowercase().contains("rj") }.sumOf { it.first }
    val b2Ishan = b2.filter { it.second.lowercase().contains("ishan") }.sumOf { it.first }
    val b2OtherFam = totalFamilyExp - (b2YJ + b2SSJ + b2PSJ + b2RJ + b2Ishan)

    // Find list of items in b2 having a sum close to 89250, 86275, or 2975
    val b2Items = b2.toList()
    println("DIAGNOSTIC - Searching b2 for precise matches:")
    // 1. Single items
    b2Items.forEach { (amt, desc) ->
        if (Math.abs(amt - 89250.0) < 5.0) {
            println("b2: Single item close to 89250: $amt - $desc")
        }
        if (Math.abs(amt - 86275.0) < 5.0) {
            println("b2: Single item close to 86275: $amt - $desc")
        }
        if (Math.abs(amt - 2975.0) < 5.0) {
            println("b2: Single item close to 2975: $amt - $desc")
        }
    }

    // 2. Pair of items
    for (i in b2Items.indices) {
        val (a1, d1) = b2Items[i]
        for (j in i + 1 until b2Items.size) {
            val (a2, d2) = b2Items[j]
            if (Math.abs(a1 + a2 - 89250.0) < 5.0) {
                println("b2: Pair sum close to 89250: $a1 ($d1) + $a2 ($d2) = ${a1+a2}")
            }
            if (Math.abs(a1 - a2 - 89250.0) < 5.0) {
                println("b2: Pair diff close to 89250: $a1 ($d1) - $a2 ($d2) = ${a1-a2}")
            }
            if (Math.abs(a2 - a1 - 89250.0) < 5.0) {
                println("b2: Pair diff close to 89250: $a2 ($d2) - $a1 ($d1) = ${a2-a1}")
            }
            
            if (Math.abs(a1 + a2 - 86275.0) < 5.0) {
                println("b2: Pair sum close to 86275: $a1 ($d1) + $a2 ($d2) = ${a1+a2}")
            }
            if (Math.abs(a1 - a2 - 86275.0) < 5.0) {
                println("b2: Pair diff close to 86275: $a1 ($d1) - $a2 ($d2) = ${a1-a2}")
            }
            
            if (Math.abs(a1 + a2 - 2975.0) < 5.0) {
                println("b2: Pair sum close to 2975: $a1 ($d1) + $a2 ($d2) = ${a1+a2}")
            }
        }
    }

    // Detailed print of each family member individual sum
    val individualNames = listOf("yj", "ssj", "sjj", "psj", "rj", "ishan", "home", "baa", "papa", "didi", "chintan patel")
    println("DIAGNOSTIC - Individual family members totals:")
    individualNames.forEach { name ->
        val inc = b3.filter { it.second.lowercase().contains(name) }.sumOf { it.first }
        val exp = b2Items.filter { it.second.lowercase().contains(name) }.sumOf { it.first }
        val b1Exp = b1.filter { it.second.lowercase().contains(name) }.sumOf { it.first }
        val b4Exp = b4.filter { it.second.lowercase().contains(name) }.sumOf { it.first }
        val totalExpForName = exp + b1Exp + b4Exp
        println("Member '$name': Income = $inc, Expense = $totalExpForName, Net = ${inc - totalExpForName}")
    }

    println("DIAGNOSTIC - Searching for other target categories:")
    // Print sum of transactions containing specific keywords
    val keywords = listOf("yj", "ssj", "psj", "rj", "ishan", "home", "baa", "papa", "didi", "laptop", "college", "rent", "salary")
    keywords.forEach { kw ->
        val sumEx = b2Items.filter { it.second.lowercase().contains(kw) }.sumOf { it.first }
        val sumIn = b3.filter { it.second.lowercase().contains(kw) }.sumOf { it.first }
        println("Keyword '$kw': Expenses Sum = $sumEx, Incomes Sum = $sumIn, Net = ${sumIn - sumEx}")
    }

    println("DIAGNOSTIC - Searching for net equations:")
    
    // Family Net values
    val nets = mapOf<String, Double>(
        "yjNet" to (b3YJ - b2YJ),
        "ssjNet" to (b3SSJ - b2SSJ),
        "psjNet" to (b3PSJ - b2PSJ),
        "rjNet" to (b3RJ - b2RJ),
        "ishanNet" to (b3Ishan - b2Ishan),
        "otherFamNet" to (b3OtherFam - b2OtherFam)
    )

    // Base non-family balance is: totalNonFamilyInc - totalNonFamilyExp - sumB1 - sumB4
    val baseNonFam = totalNonFamilyInc - totalNonFamilyExp - sumB1 - sumB4
    println("Base Non-Family Balance including B1 and B4: $baseNonFam")

    val netList = nets.toList()
    fun searchNets(idx: Int, currentSum: Double, path: List<String>) {
        if (idx == netList.size) {
            val total = baseNonFam + currentSum
            if (Math.abs(total - 86275.0) < 150000.0) {
                println("CLOSE TO 86275 WITH NETS: total=$total, path=$path")
            }
            if (Math.abs(total - (-2975.0)) < 150000.0) {
                println("CLOSE TO -2975 WITH NETS: total=$total, path=$path")
            }
            return
        }
        val (name, value) = netList[idx]
        // 0 (excluded)
        searchNets(idx + 1, currentSum, path)
        // +1 (included)
        searchNets(idx + 1, currentSum + value, path + "+$name")
        // -1 (inverted)
        searchNets(idx + 1, currentSum - value, path + "-$name")
    }
    searchNets(0, 0.0, emptyList())
  }
}

