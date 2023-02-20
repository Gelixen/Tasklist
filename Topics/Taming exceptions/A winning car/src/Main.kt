import java.lang.Exception

fun findCar(): String {
    val maxSpeed = readln().toInt()
    val accTime = readln().toInt()

    if (accTime < 1) throw Exception("The race can't be won with this car")
    
    return if (maxSpeed >= 120 && accTime < 4) "I will definitely win!"
    else throw Exception("The race can't be won with this car")
    // write your code here

}
